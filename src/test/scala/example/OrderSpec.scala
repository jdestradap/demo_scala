package example

import java.time.Instant
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.funsuite.AnyFunSuite
import JsonCodecs._

class OrderSpec extends AnyFunSuite {

  test("should deserialize valid JSON into Order") {
    val jsonString = """
    {
      "orderId": "12345",
      "date": "2024-09-04T12:34:56Z",
      "storeId": "67890",
      "countryCode": "US",
      "value": 199.99
    }
    """

    val expectedOrder = Order(
      orderId = "12345",
      date = Instant.parse("2024-09-04T12:34:56Z"),
      storeId = "67890",
      countryCode = "US",
      value = 199.99
    )

    val result = decode[Order](jsonString)

    assert(result == Right(expectedOrder))
  }

  test("should fail to deserialize invalid JSON") {
    val invalidJsonString = """
    {
      "orderId": "12345",
      "date": "invalid-date-format",
      "storeId": "67890",
      "countryCode": "US",
      "value": "not-a-number"
    }
    """

    val result = decode[Order](invalidJsonString)

    assert(result.isLeft)
  }
}
