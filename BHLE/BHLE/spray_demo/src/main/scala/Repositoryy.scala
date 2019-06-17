import akka.actor.{Actor, ActorLogging, Props}

object Repositoryy {
    def props: Props = Props[Repositoryy]
}
class Repositoryy extends Actor with ActorLogging {
    val patientDao = new PatientDAO()
    patientDao.init_()

    override def receive: Receive = {
        case GetPatients() =>  sender() ! Patients(patientDao.getPatients)
        case GetPatient() => sender() ! patientDao.getPatient
    }
}
