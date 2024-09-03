package example

import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import akka.kafka.Subscriptions
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import akka.stream.{ActorMaterializer, Materializer, SystemMaterializer}

import scala.concurrent.ExecutionContextExecutor

object KafkaStreamApp extends App {
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
    .plainSource(consumerSettings, Subscriptions.topics("test-topic"))

  // Sink: log the messages
  kafkaSource
    .map(record => s"Consumed message: ${record.value}")
    .runWith(Sink.foreach(println))
    .onComplete(_ =>
        system.terminate())
}