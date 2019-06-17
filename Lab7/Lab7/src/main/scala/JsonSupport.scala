import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import models.{Drug, Drugs, Pharmacy, Response, Buyer, Buyers}

trait JsonSupport {
  implicit val drugFormat: RootJsonFormat[Drug] = jsonFormat4(Drug)
  implicit val drugsFormat: RootJsonFormat[Drugs] = jsonFormat1(Drugs)
  implicit val pharmacyFormat: RootJsonFormat[Pharmacy] = jsonFormat3(Pharmacy)
  implicit val buyerFormat: RootJsonFormat[Buyer] = jsonFormat3(Buyer)
  implicit val buyersFormat: RootJsonFormat[Buyers] = jsonFormat1(Buyers)
  implicit val responseFormat: RootJsonFormat[Response] = jsonFormat1(Response)
}
