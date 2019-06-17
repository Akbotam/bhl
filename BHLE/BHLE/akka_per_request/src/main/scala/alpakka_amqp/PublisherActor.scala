package alpakka_amqp


import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.alpakka.amqp.{AmqpConnectionProvider, OutgoingMessage}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.rabbitmq.client.AMQP.BasicProperties
import com.typesafe.config.Config
import org.json4s.jackson.Serialization._
import serializers.Serializers

import scala.collection.JavaConverters
import scala.concurrent.duration._

object PublisherActor {

    case class PublishToQueue(dm: DomainMessage[_], correlationId: Option[String] = None)

    case class Success(correlationId: Option[String] = None)

    case class Failure(reason: Throwable, correlationId: Option[String] = None)

    def props()(implicit
                materializer: ActorMaterializer,
                amqpConnection: AmqpConnectionProvider,
                config: Config): Props =
        Props(new PublisherActor())
}

class PublisherActor()(implicit
                       materializer: ActorMaterializer,
                       amqpConnection: AmqpConnectionProvider,
                       config: Config)
  extends Actor
    with ActorLogging
    with Serializers
    with AmqpConsumerSinkDeclaration {

    import PublisherActor._
    import context._

    val amqpSink = createAmqpSink(config)

    val reconnectionDelay = config.getInt("amqp.connection.reconnectionDelay").seconds

    /**
      * run - runs publisher stream
      */
    val streamQueue: SourceQueueWithComplete[OutgoingMessage] = {

        // Source of OutgoingMessage queue
        val rabbitOutgoingSource: Source[OutgoingMessage, SourceQueueWithComplete[OutgoingMessage]] =
            Source.queue[OutgoingMessage](80, OverflowStrategy.backpressure)

        // graph
        val graph: RunnableGraph[SourceQueueWithComplete[OutgoingMessage]] = rabbitOutgoingSource.to(amqpSink)

        // run graph - SourceQueue[OutgoingMessage]
        graph.run()

    }

    override def receive = {

        case PublishToQueue(dm, correlationId) =>

            val body: String = write(dm)
            log.info(s"Publishing message [routingKey=${dm.routingKey}]: $body")

            val replyTo = dm match {
                case req: Request[_] => req.replyTo
                case _ => None
            }

            val senderRef = sender()

            val outgoingMessage = OutgoingMessage(
                ByteString(body),
                immediate = false,
                mandatory = false,
                props = Some(
                    new BasicProperties.Builder()
                      .contentType("application/json")
                      .correlationId(correlationId.orNull)
                      .replyTo(replyTo.orNull)
                      .headers(JavaConverters.mapAsJavaMap(dm.headers.map(m => (m._1, m._2.asInstanceOf[AnyRef]))))
                      .build()
                ),
                routingKey = Some(dm.routingKey)
            )

            streamQueue.offer(outgoingMessage) onComplete {
                case scala.util.Success(s) =>
                    senderRef ! PublisherActor.Success(correlationId)
                case scala.util.Failure(ex) =>
                    log.error(s"Failed to publish message: ${ex.getMessage}")
                    ex.printStackTrace()
                    senderRef ! PublisherActor.Failure(ex, correlationId)
            }

    }

}