package alpakka_amqp

import akka.Done
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, ReceiveTimeout}
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import alpakka_amqp.AMQPPerRequest.HandleMessage
import domain._
import org.json4s.JValue
import serializers.Serializers

import scala.concurrent.duration._

trait AMQPPerRequest extends Actor with ActorLogging with TransformUtils with OneError {

    case object Ack

    /**
      * Rejects message (sends back to the queue) and stops the actor
      */
    case object Reject
    /**
      * Sends acknowledge to AMQP and stops the actor
      */

    import context._

    def cim: CommittableIncomingMessage
    def target: ActorRef
    def publisher: ActorRef

    def ttl: Duration

    var requestContext: AmqpRequestContext = _

    setReceiveTimeout(ttl)
    log.info(s"$cim")
    try {

        val body = parseBody(cim.message)

        val lang = readLanguageFromIncomingMessage(cim.message, body)

        log.info(s"${getRoutingKey(cim.message)}")

        requestContext = AmqpRequestContext(
            correlationId = getCorrelationId(cim.message),
            replyTo = getReplyTo(cim.message, body),
            language = lang,
            listener = None,
            deliveryTag = getDeliveryTag(cim.message),
            stan = getStan(cim.message, body),
            headers = getHeaders(cim.message, body)
        )

        target ! HandleMessage(
            body,
            getRoutingKey(cim.message),
            requestContext
        )

    } catch {
        case e =>
            log.info(s"Unable to parse request body: ${e.getMessage}")
            e.printStackTrace()
            completeWithError(e)
    }

    def receive = {
        case Ack | Done =>
            stopRequest()
        case Reject =>
            rejectRequest()
        case entity: DomainEntity =>
            complete(entity)
        case res: Response[DomainEntity] =>
            complete(res)
        case ex: ApiException =>
            completeWithErrorInfo(apiExceptionToErrorInfo(ex, requestContext.language))
        case ReceiveTimeout =>
            completeWithError(
                GatewayTimeoutErrorException(OneErrorCode.TIMEOUT_ERROR(errorSeries, errorSystem))
                //        false
            )

        case er: ErrorInfo =>
            log.error(s"Completing request with error: $er")
            completeWithErrorInfo(er)

        case e: Throwable =>
            completeWithError(e)
    }

    /**
      * Stops request (current message to marked as processed)
      */
    def stopRequest() = {

        cim.ack()
        stop(self)

    }

    /**
      * Stops request (current message returned back to the queue)
      */
    def rejectRequest() = {

        cim.nack(requeue = true)
        stop(self)

    }

    def complete(domainEntity: DomainEntity) = {

        if (requestContext.replyTo.isDefined) {
            publisher ! PublisherActor.PublishToQueue(
                Response(
                    requestContext.headers,
                    domainEntity,
                    requestContext.replyTo.get
                ),
                Some(requestContext.correlationId)
            )
        } else {
            log.warning("Routing key is not defined(replyTo not found in AMQP message properties and in body). Just stopping request")
        }

        cim.ack()
        stop(self)

    }

    def complete[T <: DomainEntity](response: Response[T]) = {

        publisher ! PublisherActor.PublishToQueue(
            response,
            Some(requestContext.correlationId)
        )

        cim.ack()
        stop(self)

    }

    def completeWithErrorInfo(errorInfo: ErrorInfo, ack: Boolean = true): Unit = {

        log.error(s"Completing rabbit request with error: $errorInfo \n ack is $ack")

        val dm = DomainMessage.error(
            errorInfo,
            requestContext.replyTo,
            requestContext.headers
        )

        publisher ! PublisherActor.PublishToQueue(dm, Some(requestContext.correlationId))

        if (ack) {
            cim.ack()
        } else {
            cim.nack(requeue = true)
        }
        stop(self)

    }

    def completeWithError(e: Throwable, ack: Boolean = true): Unit = {
        completeWithErrorInfo(throwableToErrorInfo(e, requestContext.language), ack)
    }

    override val supervisorStrategy = OneForOneStrategy() {

        case e: ApiException => {

            completeWithError(e, ack = true)
            Stop

        }

        case e => {

            completeWithError(e)
            Stop

        }
    }

}

object AMQPPerRequest {

    case class HandleMessage(body: JValue, routingKey: String, context: AmqpRequestContext)

    case class WithActorRef(cim: CommittableIncomingMessage, target: ActorRef, publisher: ActorRef, ttl: Duration = 60.seconds)
      extends AMQPPerRequest

    case class WithProps(
                          cim: CommittableIncomingMessage,
                          props: Props,
                          publisher: ActorRef,
                          ttl: Duration = 60.seconds
                        ) extends AMQPPerRequest {
        lazy val target = context.actorOf(props)
    }

}

trait AMQPPerRequestCreator {
    this: Actor =>

    import AMQPPerRequest._

    def perRequest(cim: CommittableIncomingMessage, target: ActorRef, publisher: ActorRef, ttl: Duration) =
        context.actorOf(Props(new WithActorRef(cim, target, publisher, ttl)))

    def perRequest(cim: CommittableIncomingMessage, target: ActorRef, publisher: ActorRef) =
        context.actorOf(Props(new WithActorRef(cim, target, publisher)))

    def perRequest(cim: CommittableIncomingMessage, props: Props, publisher: ActorRef, ttl: Duration) =
        context.actorOf(Props(new WithProps(cim, props, publisher, ttl)))

    def perRequest(cim: CommittableIncomingMessage, props: Props, publisher: ActorRef) =
        context.actorOf(Props(new WithProps(cim, props, publisher)))
}