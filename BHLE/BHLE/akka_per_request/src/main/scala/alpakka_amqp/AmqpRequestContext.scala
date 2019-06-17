package alpakka_amqp

import akka.actor.ActorRef

case class AmqpRequestContext(
                               correlationId: String,
                               replyTo: Option[String] = None,
                               language: String,
                               listener: Option[ActorRef] = None,
                               deliveryTag: Long,
                               stan: String,
                               headers: Map[String, String] = Map()
                             )