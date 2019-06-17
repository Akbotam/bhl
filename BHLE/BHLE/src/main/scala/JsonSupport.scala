import abstractClasses.Message
import actors.Administrator.Employees
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import model.{CustomerModel, EmployeeModel, Session}
import spray.json.DefaultJsonProtocol._
import spray.json.{RootJsonFormat, _}


trait JsonSupport {

    implicit val libraryResponseFormat: RootJsonFormat[Message] = jsonFormat1(Message)
    implicit val bookModelFormat: RootJsonFormat[EmployeeModel] = jsonFormat5(EmployeeModel)
    implicit val libraryBooksFormat = jsonFormat1(Employees)
    implicit val sessionsFormat = jsonFormat3(Session)
    implicit val customerFormat: RootJsonFormat[CustomerModel] = jsonFormat4(CustomerModel)
}