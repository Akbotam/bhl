package alpakka_amqp

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.stream.alpakka.amqp.{ AmqpConnectionProvider, BindingDeclaration, Declaration, ExchangeDeclaration, NamedQueueSourceSettings, QueueDeclaration }
import akka.stream.alpakka.amqp.scaladsl.{ AmqpSource, CommittableIncomingMessage }
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import akka.stream.{ ActorMaterializer, ClosedShape, Outlet }
import akka.{ Done, NotUsed }
import com.typesafe.config.{ Config, ConfigException }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class AmqpConsumer(listener: Either[(CommittableIncomingMessage) => Props, ActorRef], configKey: String,
                   queueName: Option[String] = None, keyName: Option[String] = None)(implicit
                                                                                     system: ActorSystem,
                                                                                     materializer: ActorMaterializer,
                                                                                     connection: AmqpConnectionProvider,
                                                                                     config: Config) {

    val queue = if (queueName.isDefined) queueName.get else config.getString(s"amqp.consumer.${configKey}.queue")

    val key = Try {
        if (keyName.isDefined) keyName.get else config.getString(s"amqp.consumer.${configKey}.key")
    }.toOption

    val keys = Try {
        config.getStringList(s"amqp.consumer.$configKey.keys").asScala
    }.toOption

    if (key.isEmpty && keys.isEmpty) {
        throw new RuntimeException("Consumer keys are not defined!")
    }
    system.log.debug(s"${key}")

    val passiveQueue = config.getBoolean(s"amqp.consumer.${configKey}.passiveQueue")
    val passiveExchange = config.getBoolean(s"amqp.consumer.${configKey}.passiveExchange")
    val bindToExchange = config.getString(s"amqp.consumer.${configKey}.retryExchange")
    val qos = config.getInt(s"amqp.consumer.${configKey}.qos")
    val exchangeType = config.getString(s"amqp.consumer.${configKey}.exchangeType")
    val durable = try { config.getBoolean(s"amqp.consumer.${configKey}.durableQueue") } catch { case e: ConfigException.Missing => true }
    val autoDelete = try { config.getBoolean(s"amqp.consumer.${configKey}.autoDeleteQueue") } catch { case e: ConfigException.Missing => false }
    val exclusive = try { config.getBoolean(s"amqp.consumer.${configKey}.exclusive") } catch { case e: ConfigException.Missing => false }
    val internalExchange = Try { config.getBoolean(s"amqp.consumer.${configKey}.internalExchange") }.getOrElse(false)
    val reconnectionDelay = config.getInt("amqp.connection.reconnectionDelay").seconds

    val queueDeclaration = QueueDeclaration(
        name = queue,
        durable = durable,
        exclusive = exclusive,
        autoDelete = autoDelete
    )

    system.log.info(s"${QueueDeclaration}")

    val exchangeDeclaration = ExchangeDeclaration(
        name = bindToExchange,
        exchangeType = exchangeType,
        durable = true,
        autoDelete = false,
        internal = internalExchange
    )

    system.log.info(s"${ExchangeDeclaration}")

    val keysDeclarations: Seq[Declaration] = key match {
        case Some(k) =>
            Seq(BindingDeclaration(queue, bindToExchange, routingKey = Some(k)))
        case _ =>
            keys.get.map(k => BindingDeclaration(queue, bindToExchange, routingKey = Some(k)))
    }
    val declarations: Seq[Declaration] = Seq(queueDeclaration, exchangeDeclaration) ++ keysDeclarations

    val amqpSource: Source[CommittableIncomingMessage, NotUsed] =
        RestartSource.withBackoff(reconnectionDelay, reconnectionDelay, 0) { () =>
            AmqpSource.committableSource(
                NamedQueueSourceSettings(connection, queue)
                  .withDeclarations(declarations: _*),
                bufferSize = qos
            )
        }

    /**
      * Run Consumer
      * @param busConsumerSink - Sink for Consumer output
      *
      * @return
      */
    def run(busConsumerSink: Sink[CommittableIncomingMessage, Future[Done]]) = {
        // SourceShape - like a Configurable rule for Amqp connection
        val rabbitIncomingSource = RunnableGraph.fromGraph(GraphDSL.create(busConsumerSink) { implicit builder => (sink) =>
            // builder add source stage
            val amqpOut: Outlet[CommittableIncomingMessage] = builder.add(amqpSource).out
            // mat
            amqpOut ~> sink.in
            // Make closed graph
            ClosedShape
        })
        // run create reference consumer graph
        rabbitIncomingSource.run()
    }

    run(Sink.foreach(message => {
        listener match {
            case Left(l) =>
                system.actorOf(
                    l(message)
                )
            case Right(listener) => listener ! message
        }

    }))

}
