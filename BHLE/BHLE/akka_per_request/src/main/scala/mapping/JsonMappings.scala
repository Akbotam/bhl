package mapping

import Actors._
import messages.ActionPerformed
import model.{FullPatient, Patient}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonMappings extends SprayJsonSupport  {
    import DefaultJsonProtocol._
    implicit val patientFormat = jsonFormat7(Patient)
    implicit val fullPatientFormat = jsonFormat6(FullPatient)
    implicit val patientsFormat = jsonFormat1(Patients)
    implicit val ActionPerformedF = jsonFormat1(ActionPerformed)

}