import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Boot extends App {
    implicit val system = ActorSystem("spray")
    val myServiceActor = system.actorOf(Props[RestRouting], "rest-routing")
    IO(Http) ! Http.Bind(myServiceActor, interface = "localhost", 8080)
}
