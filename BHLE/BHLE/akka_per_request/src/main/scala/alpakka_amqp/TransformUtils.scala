package alpakka_amqp

import java.util.Locale

import akka.stream.alpakka.amqp.IncomingMessage
import com.fasterxml.jackson.databind.ser.Serializers
import org.json4s.JValue
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConverters._
import scala.util.Try

trait TransformUtils extends Serializers {
    val defaultLanguage = "en"

    /**
      * getLocalizedLanguage - choose possible language
      *
      * @param headerLanguage - concrete or default language
      * @return
      */
    def getLocalizedLanguage(headerLanguage: String): String = {
        if (LocalizedMessages.locales.contains(headerLanguage)) headerLanguage else defaultLanguage
    }

    /**
      * ApiException to ErrorInfo
      *
      * @param apiException - ApiException
      */
    def apiExceptionToErrorInfo(apiException: ApiException, lang: String): ErrorInfo = {
        apiException.getErrorInfo(ErrorLocaleContextFactory.getContextForLocale(new Locale(lang)))
    }

    /**
      * Throwable to ErrorInfo
      *
      * @param e - Exception
      */
    def throwableToErrorInfo(e: Throwable, lang: String): ErrorInfo = {
        val apiException = ServerErrorRequestException(OneErrorCode.INTERNAL_SERVER_ERROR(errorSeries, errorSystem), Some(e.getMessage))
        apiException.getErrorInfo(ErrorLocaleContextFactory.getContextForLocale(new Locale(lang)))
    }

    /**
      * Reads requested language from message properties headers.
      * If not presented, then default language is returned
      *
      * @param message - incoming message
      * @return language as string
      */
    def readLanguageFromIncomingMessage(message: IncomingMessage, body: JValue): String = {

        var lang = "en"

        if (message.properties != null && message.properties.getHeaders != null) {

            val iter = message.properties.getHeaders.entrySet().iterator()

            while (iter.hasNext) {

                val entry = iter.next()

                entry.getKey.toLowerCase match {
                    case "accept-language" => lang = getLocalizedLanguage(entry.getValue.toString)
                    case _ => ()
                }

            }

        }

        lang

    }

    def getCorrelationId(message: IncomingMessage): String = {
        message.properties.getCorrelationId
    }

    def parseBody(message: IncomingMessage): JValue = {
        parse(message.bytes.utf8String)
    }

    def getDeliveryTag(message: IncomingMessage): Long = {
        message.envelope.getDeliveryTag
    }

    def getReplyTo(message: IncomingMessage, body: JValue): Option[String] = {
        if (message.properties.getReplyTo != null) {
            Some(message.properties.getReplyTo)
        } else {
            Try { (body \ "replyTo").extract[String] }.toOption
        }
    }

    def getStan(message: IncomingMessage, body: JValue): String = {

        var stan = System.nanoTime().toString

        if (message.properties != null && message.properties.getHeaders != null) {

            val iter = message.properties.getHeaders.entrySet().iterator()

            while (iter.hasNext) {

                val entry = iter.next()

                entry.getKey.toLowerCase match {
                    case "stan" => stan = entry.getValue.toString
                    case _ => ()
                }

            }

        }

        stan

    }

    //  def getUserContext(body: JValue): Option[UserContext] = {
    //
    //    (body \ "userContext").extract[Option[UserContext]]
    //
    //  }

    def getHeaders(message: IncomingMessage, body: JValue): Map[String, String] = {

        val headers1: Map[String, String] = if (message.properties != null && message.properties.getHeaders != null) {

            message.properties.getHeaders.asScala.map {
                m =>
                    m._2 match {
                        case str: String => (m._1, str.asInstanceOf[String])
                        case any => (m._1, any.toString)
                    }
            }.toMap

        } else {
            Map()
        }

        val headers2 = (body \ "headers").extract[Map[String, String]]

        headers1 ++ headers2

    }

    def getRoutingKey(message: IncomingMessage): String = {
        message.envelope.getRoutingKey
    }

}