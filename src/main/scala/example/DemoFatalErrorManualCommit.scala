package example
import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{ActorAttributes, Materializer, Supervision, SystemMaterializer}
import io.circe.jawn.decode
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object DemoFatalErrorManualCommit extends App {
  implicit val system: ActorSystem = ActorSystem("KafkaStreamSystem")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  import JsonCodecs._

  private val bootstrapServers: String = Option(System.getenv("KAFKA_BOOTSTRAP_SERVERS")) match {
    case Some(value) if value.trim.nonEmpty => value
    case _ => "localhost:9092"
  }

  private val consumerSettings: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withGroupId("akka-stream-kafka-group-error")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val decider: Supervision.Decider = {
    case ex: Exception =>
      println(s"Supervision strategy triggered for exception: ${ex.getMessage}")
      Supervision.Stop
    case _ => Supervision.Resume
  }

  val committerSettings: CommitterSettings = CommitterSettings(system)

  private val kafkaSource = Consumer
    .committableSource(consumerSettings, Subscriptions.topics("test-topic"))

  val processingFlow = Flow[CommittableMessage[String, String]]
    .mapAsync(1) { msg =>
      decode[Order](msg.record.value) match {
        case Right(order) =>
          TaxCalculatorService.calculateTotalAmount(order) match {
            case Right(newOrder) =>
              println(s"Successfully processed order: $newOrder")
              Future.successful(msg.committableOffset)
            case Left(error) =>
              println(s"Error processing order: $error")
              Future.successful(msg.committableOffset)
          }
        case Left(error) =>
          println(s"Failed to deserialize JSON: $error")
          Future.failed(new RuntimeException(s"Failed to deserialize JSON: $error"))
      }
    }

  val streamCompletion: Future[Done] = kafkaSource
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .throttle(elements = 100, per = 60.seconds)
    .via(processingFlow)
    .via(Committer.flow(committerSettings))
    .runWith(Sink.ignore)

  Await.result(system.whenTerminated, Duration.Inf)
}
