trait RestMessage

case class GetPatients() extends RestMessage
case class GetPatient() extends RestMessage
case class ActionPerformed(msg: String) extends RestMessage {
    def getMsg = msg
}
