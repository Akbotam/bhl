package serializers

import model.Patient
import org.json4s.{ShortTypeHints, jackson}
import org.json4s.native.Serialization

trait Serializers {

    implicit val formats = Serialization.formats(

        ShortTypeHints(List(classOf[Patient], classOf[Patient], classOf[Request[_]], classOf[Response[_]]))

    )

    implicit val serialization = jackson.Serialization
}