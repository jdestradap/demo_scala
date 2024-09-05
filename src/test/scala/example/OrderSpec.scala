package example

import java.time.Instant
import io.circe._
import io.circe.parser._
import org.scalatest.funsuite.AnyFunSuite
import JsonCodecs._

class OrderSpec extends AnyFunSuite {

  test("should deserialize valid JSON into Order with optional fields present") {
    val jsonString = """
    {
      "orderId": "12345",
      "valueWithoutTaxes": 199.99,
      "countryCode": "US",
      "state": "CA",
      "totalAmount": 219.99
    }
    """

    val expectedOrder = Order(
      orderId = "12345",
      valueWithoutTaxes = 199.99,
      countryCode = "US",
      state = Some("CA"),
      totalAmount = Some(219.99)
    )

    val result = decode[Order](jsonString)

    assert(result == Right(expectedOrder))
  }

  test("should deserialize valid JSON into Order with missing optional fields") {
    val jsonString = """
    {
      "orderId": "12345",
      "valueWithoutTaxes": 199.99,
      "countryCode": "US"
    }
    """

    val expectedOrder = Order(
      orderId = "12345",
      valueWithoutTaxes = 199.99,
      countryCode = "US",
      state = None,
      totalAmount = None
    )

    val result = decode[Order](jsonString)

    assert(result == Right(expectedOrder))
  }

  test("should fail to deserialize invalid JSON with wrong types") {
    val invalidJsonString = """
    {
      "orderId": "12345",
      "valueWithoutTaxes": "not-a-number",
      "countryCode": "US"
    }
    """

    val result = decode[Order](invalidJsonString)

    assert(result.isLeft)
  }
}
