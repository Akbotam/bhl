package AbstractClasses

import Actors.{PatientManagerActor, Patients}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import messages.GetPatients

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object UserFactory {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val pActor: ActorRef = system.actorOf(PatientManagerActor.props, "PatientActor")
    implicit lazy val timeout: Timeout = Timeout(10.seconds)
    def getUser (username : String, password: String): Option[User] ={
        if (username.contains('_')) {
            val action = (pActor ? GetPatients()).mapTo[Patients]
            action.onComplete{
                case Success(value) => {
                    for(patient <- value.patients){
                        if (patient.login(username, password))
                            patient
                        else
                            LoginError()
                    }
                }
                case Failure(exception) => LoginError()
            }
        }
        else {
            LoginError()
        }
        None
    }

}