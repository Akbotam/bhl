package perrequest

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.{Route, RouteResult}
import mapping.JsonMappings
import messages.RestMessage
import per_request.PerRequestCreator

import scala.concurrent.Promise

trait RequestHandler extends PerRequestCreator with JsonMappings {
    def actorSystem: ActorSystem

    def handleHttpRequest(targetProps: Props, request: RestMessage): Route = context => {
        val promise = Promise[RouteResult]
        println("2")
        perRequest(context, targetProps, request, promise)(actorSystem)
        promise.future // A promise completes the future returned by p.future.
    }
}
