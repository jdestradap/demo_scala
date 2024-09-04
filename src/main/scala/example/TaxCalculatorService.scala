package example

trait TaxCalculatorService {
  def calculateTotalAmount(order: Order): Either[String, Order]

}

object TaxCalculatorService {
  // Factory method for creating the default service
  def default: TaxCalculatorService = new DefaultTaxCalculatorService

  // Helper function to get tax rates (can be used internally or for other services)
  def getTaxRate(order: Order): Option[TaxRate] = {
    order.countryCode match {
      case "US" => order.state.flatMap(state => getUSTaxRate(state))
      case "CA" | "PR" => Some(TaxRate(0.15))  // Fixed tax for Canada or Puerto Rico
      case _ => None
    }
  }

  private def getUSTaxRate(state: String): Option[TaxRate] = {
    val stateTaxRates = Map(
      "CA" -> TaxRate(0.08),
      "NY" -> TaxRate(0.07),
      "TX" -> TaxRate(0.06)
    )
    stateTaxRates.get(state)
  }
}

class DefaultTaxCalculatorService extends TaxCalculatorService {
  override def calculateTotalAmount(order: Order): Either[String, Order] = {
    TaxCalculatorService.getTaxRate(order) match {
      case Some(taxRate) =>
        val totalAmount = order.valueWithoutTaxes * (1 + taxRate.rate)
        Right(order.withTotalAmount(totalAmount))
      case None => Left("Unable to determine tax rate for the given order.")
    }
  }
}

case class TaxRate(rate: Double)