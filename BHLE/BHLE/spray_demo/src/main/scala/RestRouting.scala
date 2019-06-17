import akka.actor.{Actor, Props}
import spray.httpx.Json4sSupport
import spray.routing.{HttpServiceActor, Route}
import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import org.json4s.{DefaultFormats, Formats}

import language.postfixOps

object RestRouting {
    def props(): Props = Props(new RestRouting)
}

class RestRouting extends HttpServiceActor with PerRequestCreator with Json4sSupport {

    override def actorRefFactory = context

    val repositoryy = actorRefFactory.actorOf(Repositoryy.props, "patient-repository")

    override def receive: Receive = runRoute(route)

    def route : Route = {
        get {
            path("patient") {
                getPatientHandler(GetPatient())
            }
        }

    }

    def getPatientHandler(msg: RestMessage): Route =  {
        ctx => perRequest(ctx, repositoryy, msg)
    }

    override implicit def json4sFormats: Formats = DefaultFormats
}