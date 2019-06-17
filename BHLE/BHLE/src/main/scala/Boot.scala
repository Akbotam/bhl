import abstractClasses.Message
import actors.Administrator
import actors.Administrator.Employees
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.directives.PathDirectives.path
import model.{CustomerModel, EmployeeModel, Session}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._


object  Boot  extends  App  with JsonSupport {

    // needed to run the route
    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()
    // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
    implicit val executionContext = system.dispatcher

    implicit val timeout = Timeout(30.seconds)

    val admin = system.actorOf(Administrator.props("1", "aida", "ospanova08", "uljanek06", 124), "admin")

    lazy val routes = {
        pathPrefix("Paradise") {
            path("employee") {
                get {
                    parameters('key.as[String]) { s =>


                        println("GET")
                        if (s == "1")
                            complete {
                                (admin ? Administrator.GetEmployees()).mapTo[Employees]
                            }
                        else
                            complete {
                                (admin ? Administrator.GetEmployee(s)).mapTo[EmployeeModel]
                            }

                    }



                } ~
                post {
                    entity(as[EmployeeModel]) { emp => {
                            complete {
                                (admin ? Administrator.CreateEmployee(emp)).mapTo[Message]
                            }
                        }

                    }
                } ~
                put {
                    entity(as[Session]) { s =>
                        complete {
                            (admin ? Administrator.RegisterSession(s)).mapTo[Message]
                        }

                    }
                }
            } ~
            path ("employees") {
                get{
                parameters('name.as[String], 'id.as[String]) { (name, id) =>
                    println("GET")
                    complete {
                        (admin ? Administrator.GetEmployeeWithNameId(name, id)).mapTo[EmployeeModel]
                    }

                }
                }

            } ~
            path ("customer") {
                post {
                        entity(as[CustomerModel]) { cst => {
                            complete {
                                (admin ? Administrator.CreateCustomer(cst)).mapTo[Message]
                            }
                        }
                    }
                }
            }


        }
    }
    println("BOOT")

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 9000)

}
