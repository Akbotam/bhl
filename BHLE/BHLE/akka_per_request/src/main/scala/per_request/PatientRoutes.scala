package per_request

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import mapping.JsonMappings
import messages.{GetPatients, RestMessage}
import perrequest.RequestHandler


trait PatientRoutes extends RequestHandler with JsonMappings {
    def actorSystem: ActorSystem

    def patientManager: ActorRef

    def getPatientHandler(action: RestMessage): Route = {
        println("get")
        handleHttpRequest(Props(classOf[RequestProcessingActor], patientManager), action)
    }

    val routes: Route =

        get {
            path("patients") {
                getPatientHandler(GetPatients())
            }
        }
}
