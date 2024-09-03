package example

import akka.actor.ActorSystem
import akka.kafka.{ProducerSettings, ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.{Producer, Consumer}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer, SystemMaterializer}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.ExecutionContextExecutor
import scala.util.Random

object Demo extends App {
  implicit val system: ActorSystem = ActorSystem("KafkaStreamSystem")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  // Kafka consumer settings
  val consumerSettings: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("akka-stream-kafka-group")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  // Kafka producer settings
  val producerSettings: ProducerSettings[String, String] = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")
    .withProperty(ProducerConfig.ACKS_CONFIG, "all")

  // Source: Generate random messages
  val kafkaProducerSource = Source(1 to 100)
    .map { n =>
      val key = s"key-$n"
      val value = s"message-$n-${Random.alphanumeric.take(5).mkString}"
      new ProducerRecord[String, String]("test-topic", key, value)
    }

  // Sink: Send messages to Kafka
  kafkaProducerSource
    .runWith(Producer.plainSink(producerSettings))
    .onComplete(_ =>
      println("Produced all messages"))

  // Consumer: Read from Kafka topic
  val kafkaSource = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test-topic"))

  // Sink: Log the messages consumed
  kafkaSource
    .map(record => s"Consumed message: ${record.value}")
    .runWith(Sink.foreach(println))
    .onComplete(_ =>
      system.terminate())
}
