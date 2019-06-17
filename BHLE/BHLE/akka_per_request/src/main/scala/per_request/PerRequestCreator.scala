package per_request

import java.util.UUID

import Actors.{PatientManagerActor, Patients}
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, ReceiveTimeout, SupervisorStrategy}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import mapping.JsonMappings
import messages.{ActionPerformed, RestMessage}
import model.Patient
import per_request.PerRequestActor.WithProps
import spray.json.enrichString

import scala.concurrent.Promise
import scala.concurrent.duration._

trait PerRequestCreator {
    def perRequest(r: RequestContext,
                   props: Props,
                   request: RestMessage,
                   promise: Promise[RouteResult])(implicit system: ActorSystem) : ActorRef = {
        system.actorOf(Props(classOf[WithProps], r, props, request, promise), s"pr-${UUID.randomUUID().toString}")
    }
}

object PerRequestActor {
    val emptyJson = "{}".parseJson
    val requestTimeoutResponse = ActionPerformed("Request timeout")

    case class WithProps(r: RequestContext,
                         props: Props,
                         requestMessage: RestMessage,
                         promise: Promise[RouteResult]) extends BasePerRequestActor {

        lazy val target: ActorRef = context.actorOf(PatientManagerActor.props, "target-http")
        println(target)
    }
}


trait BasePerRequestActor extends Actor with JsonMappings {
    import context._
    import context.dispatcher

    def r: RequestContext
    def target: ActorRef
    def requestMessage: RestMessage
    def promise: Promise[RouteResult]

    setReceiveTimeout(15.seconds)
    target ! requestMessage

    def complete(m: => ToResponseMarshallable): Unit = {
        val future = r.complete(m)
        future.onComplete(promise.complete(_))
        context.stop(self)
    }
    override val supervisorStrategy : SupervisorStrategy =
        OneForOneStrategy() {
            case e =>
                complete(InternalServerError, e.getMessage)
                Stop
        }
    override def receive: Receive = {
        case  patients: Patients =>
            complete(OK, patients)

        case ActionPerformed(msg: String) => complete(OK, msg)
        case msg => complete(OK, s"unknown msg = $msg")
    }


}