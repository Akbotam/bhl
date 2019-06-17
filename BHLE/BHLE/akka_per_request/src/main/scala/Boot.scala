
import Actors.PatientManagerActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import per_request.PatientRoutes

import scala.concurrent.ExecutionContext

object Main extends App with PatientRoutes  {
    implicit val actorSystem = ActorSystem("PatientSystem")
    protected implicit val executor: ExecutionContext = actorSystem.dispatcher
    protected val log: LoggingAdapter = Logging(actorSystem, getClass)
    protected implicit val materializer: ActorMaterializer = ActorMaterializer()

    override val patientManager: ActorRef = actorSystem.actorOf(Props[PatientManagerActor], "PatientManagerActor")

    Http().bind("localhost", 5000).runForeach(_.handleWith(Route.handlerFlow(routes)))

    println("Started")
}