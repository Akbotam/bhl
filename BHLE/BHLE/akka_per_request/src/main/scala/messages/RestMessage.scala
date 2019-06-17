package messages

import model.{FullPatient, Patient}

trait RestMessage

case class GetPatients() extends RestMessage

case class AddPatient(p: FullPatient) extends RestMessage
case class AddPatientTest(p: Patient) extends RestMessage
case class GetbyID(patient_id: Int) extends RestMessage
case class UpdatePatient( id: Int, newP: Patient) extends RestMessage
case class DeletePatient(id: Int) extends RestMessage
final case class ActionPerformed(description: String) extends RestMessage {
    def getMsg() = this.description
}
final case class OpFailed (msg: String) extends RestMessage
