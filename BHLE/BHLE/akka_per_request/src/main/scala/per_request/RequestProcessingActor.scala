package per_request

import Actors.Patients
import akka.actor.{Actor, ActorRef}
import messages.GetPatients
import model.Patient

class RequestProcessingActor(managerActor: ActorRef) extends Actor {
    var patients = Option.empty[Seq[Patient]]
    override def receive: Receive = {

        case GetPatients =>
            println("getgg")
            managerActor ! GetPatients
            context.become(waitingResponses) // Переключать состояний актора


    }

    def waitingResponses: Receive = {
        case Patients(bookSeq) =>
            patients = Some(bookSeq)

            if (patients.nonEmpty) {
                context.parent ! Patients(patients.get)
            }
    }
}