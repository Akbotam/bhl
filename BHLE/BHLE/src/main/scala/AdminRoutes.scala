
import actors.Administrator._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.util.Timeout
import model.EmployeeModel

import scala.concurrent.duration._



trait AdminRoutes extends JsonSupport {

     def admin: ActorRef


}
