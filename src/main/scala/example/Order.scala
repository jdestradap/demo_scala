package example

import java.time.Instant
import io.circe._, io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

// Define the Order case class
case class Order(orderId: String, date: Instant, storeId: String, countryCode: String, value: Double)

// Define an object to hold implicit decoders
object JsonCodecs {
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry(str => scala.util.Try(Instant.parse(str)))
  implicit val orderDecoder: Decoder[Order] = deriveDecoder[Order]
}

