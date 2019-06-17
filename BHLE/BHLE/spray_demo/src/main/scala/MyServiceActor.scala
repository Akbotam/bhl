import akka.actor.{Actor, ActorRefFactory}
import spray.routing.HttpService

class MyServiceActor extends Actor with HttpService  {
    def receive = runRoute(route)

    def actorRefFactory: ActorRefFactory = context
    val route = {
        pathPrefix("test") {
            pathPrefix("success") {
                get {
                    complete("Tests successed!")
                }
            } ~ pathPrefix("fail") {
                post {
                    complete("Tests failed!")
                }
            }
        }

    }
}