package alpakka_amqp

import domain.{ DomainEntity, DomainObject, ErrorInfo }

trait MessageType {
    val purpose: String // request / event/ command/ error
    val source: Option[String]
    val target: Option[String]
    val flow: Option[String] = None
    val name: String
    val routingKey: String

    def key: String = {
        val route = purpose match {
            case "request" => s"${target.get}${flow.getOrElse("").toUpperCase}"
            case "command" => s"${target.get}${flow.getOrElse("").toUpperCase}"
            //case "command" => target.get
            case "event" => s"${source.get}${flow.getOrElse("").toUpperCase}"
            case "error" => s"${source.get}${flow.getOrElse("").toUpperCase}"
            case "stream" => s"${source.get}.${flow.getOrElse("None")}"
            case _ => "none"
        }

        s"$purpose.$route.$name"
    }

    def binding: String = {
        val route = purpose match {
            case "request" => s"${target.get.toLowerCase}${flow.getOrElse("").toUpperCase}"
            case "command" => s"${target.get.toLowerCase}${flow.getOrElse("").toUpperCase}"
            //case "command" => target.get
            case "event" => s"${source.get.toLowerCase}${flow.getOrElse("").toUpperCase}"
            case "error" => s"${source.get.toLowerCase}${flow.getOrElse("").toUpperCase}"
            case "stream" => s"${source.get}.${target.get}"
            case _ => "none"
        }

        s"$purpose.$route.*"
    }
}

trait RequestMessageType extends MessageType {
    override val purpose = "request"
    override val source = None

}

trait CommandMessageType extends MessageType {
    override val purpose = "command"
    override val source = None
}

trait EventMessageType extends MessageType {
    override val purpose = "event"
    override val target = None
}

trait StreamMessageType extends MessageType {
    override val purpose = "stream"
    override val target = None
}

case class ErrorMessageType(source: Option[String], name: String) extends MessageType {
    override val purpose = "error"
    override val target = None
    override val routingKey = key
}

/**
  * Singletone to access message builders
  */
object DomainMessage {

    def error(errorInfo: ErrorInfo, replyTo: Option[String], headers: Map[String, Any]): Error = {
        val messageType = ErrorMessageType(errorInfo.system, errorInfo.code.getOrElse("undefined").toLowerCase)
        Error(headers, errorInfo, replyTo.getOrElse(messageType.key))
    }

    def error(errorInfo: ErrorInfo, commandMessage: CommandMessage[_]): Error = {

        val (replyTo: Option[String], headers: Map[String, Any]) = commandMessage match {

            case request: Request[_] => (request.replyTo, request.headers)

            case command: Command[_] => (command.replyTo, command.headers)

            case _ => (None, Map(), None)

        }

        val messageType = ErrorMessageType(errorInfo.system, errorInfo.code.getOrElse("undefined").toLowerCase)

        Error(headers, errorInfo, replyTo.getOrElse(messageType.key))

    }

    def request[T](messageType: RequestMessageType, replyTo: Option[String], headers: Map[String, Any], body: T): Request[T] = {
        Request[T](replyTo, headers, body, messageType.key)
    }

    def command[T](messageType: CommandMessageType, replyTo: Option[String], headers: Map[String, Any], body: T): Command[T] = {
        Command[T](replyTo, headers, body, messageType.key)
    }

    def streamedMessage[T](messageType: StreamMessageType, headers: Map[String, Any], body: T): StreamedMessage[T] = {
        StreamedMessage[T](headers, body, messageType.key)
    }

    /*
    def response[T](messageType: RequestMessageType,replyTo: Option[String],headers: Map[String, Any],userContext: Option[UserContext], body: T): Request[T] = {
      Request(replyTo,headers,userContext,body,replyTo.get)
    }
    */
    //  def response[T <: DomainEntity](request: Request[_], body: T): Option[Response[T]] = {
    //    if (request.replyTo.isDefined) {
    //      Some(Response[T](request.headers,request.userContext,body,request.replyTo.get))
    //    }else {
    //      None
    //    }
    //  }

    def response[T <: DomainEntity](commandMessage: CommandMessage[_], body: T): Option[Response[T]] = {

        val (replyTo: Option[String], headers: Map[String, Any]) = commandMessage match {

            case request: Request[_] => (request.replyTo, request.headers)

            case command: Command[_] => (command.replyTo, command.headers)

            case _ => (None, Map(), None)

        }

        replyTo map { replyTo => Response[T](headers, body, replyTo) }

    }

    def event[T <: DomainEntity](messageType: EventMessageType, headers: Map[String, Any], body: T): Event[T] = {
        Event[T](headers, body, messageType.key)
    }

}

/**
  * All domain messages
  */
trait DomainMessage[T] extends DomainObject {

    val headers: Map[String, Any]

    val body: T

    val routingKey: String
}

/**
  * Event messages
  */
trait EventMessage[T] extends DomainMessage[T]

/**
  * Command messages
  *
  * @tparam T - Body type
  */
trait CommandMessage[T] extends DomainMessage[T]

trait StreamMessage[T] extends DomainMessage[T]

case class Response[T <: DomainEntity](
                                        headers: Map[String, Any],
                                        body: T,
                                        routingKey: String
                                      ) extends DomainMessage[T]

case class Request[T](replyTo: Option[String], headers: Map[String, Any],
                      body: T,
                      routingKey: String) extends CommandMessage[T]

case class Command[T](replyTo: Option[String], headers: Map[String, Any],
                      body: T,
                      routingKey: String) extends CommandMessage[T]

case class StreamedMessage[T](
                               headers: Map[String, Any],
                               body: T,
                               routingKey: String
                             ) extends StreamMessage[T]

case class Event[T <: DomainEntity](
                                     headers: Map[String, Any],
                                     body: T,
                                     routingKey: String
                                   ) extends EventMessage[T]

case class Error(
                  headers: Map[String, Any],
                  body: ErrorInfo,
                  routingKey: String
                ) extends DomainMessage[ErrorInfo]

trait PersistentCommand extends DomainObject {
    val persistentId: String
}

trait PersistentEvent extends DomainObject

case class StreamedMessageError(
                                 data: String,
                                 cause: String,
                                 routingKey: String = ""
                               ) extends StreamMessage[Unit] {
    override val headers: Map[String, Any] = Map()
    override val body = ()
}

case class EcoService(system: String, subSystem: String, microService: String) extends DomainEntity {

    def serviceEndpoint = s"$microService.$subSystem.$system"
}

case class Endpoint(instanceId: String, ecoService: EcoService) extends DomainEntity {

    val serviceEndpoint = ecoService.serviceEndpoint // api.lab.it

    val instanceEndpoint = s"$instanceId.$serviceEndpoint" // localhost.api.lab.it

    def queue = s"Q:$instanceEndpoint" // Q:localhost.api.lab.it

    def exchange = s"X:$instanceEndpoint" // X:localhost.api.lab.it
}