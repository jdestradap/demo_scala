package example
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import akka.stream.{Materializer, SystemMaterializer}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Failure, Success}

object Demo extends App {
  implicit val system: ActorSystem = ActorSystem("KafkaStreamSystem")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  // Kafka consumer settings
  val consumerSettings: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("akka-stream-kafka-group")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  // Source: read from Kafka topic
  val kafkaSource = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test-topic-2"))

  // Sink: log the messages
  val streamCompletion = kafkaSource
    .map(record => s"Consumed message: ${record.value}")
    .runWith(Sink.foreach(println))

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
