package example
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import akka.stream.{Materializer, SystemMaterializer}
import io.circe.jawn.decode
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Demo extends App {
  implicit val system: ActorSystem = ActorSystem("KafkaStreamSystem")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  // Import the JsonCodecs to bring the implicit decoders into scope
  import JsonCodecs._

  // Kafka consumer settings
  private val bootstrapServers: String = Option(System.getenv("KAFKA_BOOTSTRAP_SERVERS")) match {
    case Some(value) if value.trim.nonEmpty => value
    case _ => "localhost:9092"
  }

  private val consumerSettings: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withGroupId("akka-stream-kafka-group-ok")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10") // Commit every 5 seconds
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  // Source: read from Kafka topic
  private val kafkaSource = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test-topic"))

  private val streamCompletion = kafkaSource
    .map(record => decode[Order](record.value))  // Deserialize JSON to Order
    .map {
      case Right(order) => TaxCalculatorService.calculateTotalAmount(order)
      case Left(error) => Left(s"Failed to deserialize JSON: $error")
    }
    .runWith(Sink.foreach {
      case Right(newOrder) =>
        println(s"Successfully processed order: $newOrder")
      case Left(error) =>
        println(s"Error processing order: $error")
    })

  // Handle stream completion or failure
  streamCompletion.onComplete {
    case Success(_) =>
      println("Stream completed successfully.")
      CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)
    case Failure(e) =>
      println(s"Stream failed with error: ${e.getMessage}")
      CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)
  }

  // Block the main thread to keep the application running
  Await.result(system.whenTerminated, Duration.Inf)
}
