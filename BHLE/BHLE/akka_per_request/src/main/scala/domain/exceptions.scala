package domain

import java.util.Locale

sealed trait StatusCode {
    def intValue: Int
}

object StatusCodes {

    case object OK extends StatusCode {
        override def intValue: Int = 200
    }

    case object NotFound extends StatusCode {
        override def intValue: Int = 404
    }

    case object BadRequest extends StatusCode {
        override def intValue: Int = 400
    }

    case object Unauthorized extends StatusCode {
        override def intValue: Int = 401
    }

    case object Forbidden extends StatusCode {
        override def intValue: Int = 403
    }

    case object Conflict extends StatusCode {
        override def intValue: Int = 409
    }

    case object Gone extends StatusCode {
        override def intValue: Int = 410
    }

    case object PreconditionFailed extends StatusCode {
        override def intValue: Int = 412
    }

    case object UnsupportedMediaType extends StatusCode {
        override def intValue: Int = 415
    }

    case object InternalServerError extends StatusCode {
        override def intValue: Int = 500
    }

    case object GatewayTimeout extends StatusCode {
        override def intValue: Int = 504
    }

    case object MethodNotAllowed extends StatusCode {
        override def intValue: Int = 405
    }

}

/**
  * Base Response Exception
  */
trait ApiException extends RuntimeException with Serializable {

    /**
      * Http status
      */
    def status: StatusCode

    /**
      * Exception code
      */
    def errorCode: ErrorCode

    /**
      * Exception message
      */
    def message: Option[String]

    /**
      * Returns error info
      *
      * @param errorLocaleContext
      * @return
      */
    def getErrorInfo(errorLocaleContext: Option[ErrorLocaleContext]): ErrorInfo = {
        val fullCode = getFullCode
        val errorUrl = "docs/" + fullCode
        val localizedMessage: Option[String] = errorLocaleContext match {
            case Some(context) => Some(context.getLocalizedMessage(fullCode))
            case _ => message
        }

        val developerMessage: Option[String] = getMessage match {
            case null => message
            case _ => Some(getMessage)
        }

        ErrorInfo(
            Some(errorCode.system.system),
            Some(status.intValue),
            Some(errorCode.series.series),
            Some(fullCode),
            localizedMessage,
            developerMessage,
            Some(errorUrl)
        )
    }

    /**
      * Builds full error code according
      *
      * @return Long which represents error code
      */
    def getFullCode: String = errorCode.system.system + "." + ((status.intValue * 100 + errorCode.series.series) * 1000 + errorCode.code).toString
}

/**
  * Error system
  */
trait ErrorSystem {
    val system: String
}

/**
  * Error code
  */
trait ErrorCode {
    val system: ErrorSystem
    val series: ErrorSeries
    val code: Int
}

/**
  * Error series (subsystems)
  */
trait ErrorSeries {
    val series: Int
}

/**
  * Error locale context
  */
trait ErrorLocaleContext {

    /**
      * Current locale
      */
    val locale: Locale

    /**
      * Returns localized message for the given error code
      *
      * @param fullErrorCode - error code
      * @return localized message
      */
    def getLocalizedMessage(fullErrorCode: String): String

}

/**
  * Represents error info
  *
  * @param system            - system
  * @param status            - http status
  * @param series            - exception series
  * @param code              - exception code
  * @param message           - error message (localized)
  * @param developerMessage  - message for developer
  * @param moreInfo          - some more info
  */
case class ErrorInfo(
                      system: Option[String],
                      status: Option[Int],
                      series: Option[Int],
                      code: Option[String],
                      message: Option[String],
                      developerMessage: Option[String],
                      moreInfo: Option[String]
                    )

object OneErrorSystem extends ErrorSystem {
    override val system: String = "ONE"
}

object OneErrorSeries {

    case object API extends ErrorSeries {
        override val series: Int = 1
    }
}

object OneErrorCode {

    case class TIMEOUT_ERROR(
                              override val series: ErrorSeries,
                              override val system: ErrorSystem
                            ) extends ErrorCode {
        override val code = 10
    }

    case class INTERNAL_SERVER_ERROR(
                                      override val series: ErrorSeries,
                                      override val system: ErrorSystem
                                    ) extends ErrorCode {
        override val code = 20
    }
}

case class NotFoundException(
                              override val errorCode: ErrorCode,
                              override val message: Option[String] = None
                            ) extends ApiException {
    override val status: StatusCode = StatusCodes.NotFound
}

/**
  * Bad request exception (400)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class BadRequestException(
                                override val errorCode: ErrorCode,
                                override val message: Option[String] = None
                              ) extends ApiException {
    override val status: StatusCode = StatusCodes.BadRequest
}

/**
  * Forbidden exception (403)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class ForbiddenErrorException(
                                    override val errorCode: ErrorCode,
                                    override val message: Option[String] = None
                                  ) extends ApiException {
    override val status: StatusCode = StatusCodes.Forbidden
}

/**
  * Internal server error exception (500)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class ServerErrorRequestException(
                                        override val errorCode: ErrorCode,
                                        override val message: Option[String] = None
                                      ) extends ApiException {
    override val status: StatusCode = StatusCodes.InternalServerError
}

/**
  * Unauthorized exception (401)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class UnauthorizedErrorException(
                                       override val errorCode: ErrorCode,
                                       override val message: Option[String] = None
                                     ) extends ApiException {
    override val status: StatusCode = StatusCodes.Unauthorized
}

/**
  * Gateway timeout exception (504)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class GatewayTimeoutErrorException(
                                         override val errorCode: ErrorCode,
                                         override val message: Option[String] = None
                                       ) extends ApiException {
    override val status: StatusCode = StatusCodes.GatewayTimeout
}

/**
  * Conflict exception (409)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class ConflictException(
                              override val errorCode: ErrorCode,
                              override val message: Option[String] = None
                            ) extends ApiException {
    override val status: StatusCode = StatusCodes.Conflict
}

/**
  * Gone exceptions (410)
  *
  * @param errorCode - ErrorCode
  * @param message   - optional exception message
  */
case class GoneException(
                          override val errorCode: ErrorCode,
                          override val message: Option[String] = None
                        ) extends ApiException {
    override val status: StatusCode = StatusCodes.Gone
}

/**
  * Unsupported request content type exception (415)
  *
  * @param errorCode - Error Code
  * @param message   - optional exception message
  */
case class UnsupportedContentTypeException(
                                            override val errorCode: ErrorCode,
                                            override val message: Option[String] = None
                                          ) extends ApiException {
    override val status: StatusCode = StatusCodes.UnsupportedMediaType
}

/**
  * Precondition failed exception (412)
  *
  * @param errorCode - Error Code
  * @param message   - Optional exception message
  */
case class PreconditionFailedException(
                                        override val errorCode: ErrorCode,
                                        override val message: Option[String] = None
                                      ) extends ApiException {
    override val status: StatusCode = StatusCodes.PreconditionFailed
}

case class MethodNotAllowedException(
                                      override val errorCode: ErrorCode,
                                      override val message: Option[String] = None
                                    ) extends ApiException {
    override val status: StatusCode = StatusCodes.MethodNotAllowed
}

© 2019 GitHub