import PerRequest.RequestActor
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.json4s.DefaultFormats
import spray.http.StatusCode
import spray.http.StatusCodes.OK
import spray.httpx.Json4sSupport
import spray.routing.{HttpService, RequestContext}

trait PerRequest extends  Actor with ActorLogging with Json4sSupport{
    import context._

    val json4sFormats = DefaultFormats

    def r: RequestContext
    def actor: ActorRef
    def msg: RestMessage
    actor ! msg

    override def receive: Receive = {
        case Patients(patients) => complete(OK, patients)
        case Patient(name) => complete(OK, name)
        case ActionPerformed(msg: String) => complete(OK, msg)
    }
    def complete[T](status: StatusCode, obj: T) = {
        r.complete(status, obj)
        stop(self)
    }
}
trait PerRequestCreator {
    self: HttpService =>
    def perRequest (r: RequestContext, actor : ActorRef, msg: RestMessage): Unit = {
        actorRefFactory.actorOf(Props(new RequestActor (r, actor, msg)))
    }
}
object PerRequest {
    case class RequestActor(r: RequestContext, actor: ActorRef, msg: RestMessage) extends PerRequest {}
}

