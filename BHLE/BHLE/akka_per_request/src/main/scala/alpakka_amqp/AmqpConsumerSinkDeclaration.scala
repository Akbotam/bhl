package alpakka_amqp

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.scaladsl.AmqpSink
import akka.stream.alpakka.amqp.{ AmqpConnectionProvider, AmqpSinkSettings, ExchangeDeclaration, OutgoingMessage }
import akka.stream.scaladsl._
import com.typesafe.config.Config

import scala.concurrent.duration._

trait AmqpConsumerSinkDeclaration {

    def createAmqpSink(config: Config)(implicit
                                       materializer: ActorMaterializer,
                                       connection: AmqpConnectionProvider): Sink[OutgoingMessage, NotUsed] = {

        val outboundExchange = config.getString("ecosystem.gateway.outbound.name")
        val exchangeType = config.getString("ecosystem.gateway.outbound.type")
        val reconnectionDelay = config.getInt("amqp.connection.reconnectionDelay").seconds

        val exchangeDeclaration = ExchangeDeclaration(
            outboundExchange,
            exchangeType,
            durable = true,
            autoDelete = false,
            internal = false
        )

        RestartSink.withBackoff(reconnectionDelay, reconnectionDelay, 0) { () =>
            AmqpSink(
                AmqpSinkSettings(connection).withExchange(outboundExchange).withDeclarations(exchangeDeclaration)
            )
        }
    }
}
