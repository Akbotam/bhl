package Actors

import akka.actor.{Actor, ActorLogging, Props}
import dao.PatientDAO
import messages._
import model.{FullPatient, Patient}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

final case class Patients(patients: Seq[Patient])

object PatientManagerActor {
  def props = Props(new PatientManagerActor())
}

class PatientManagerActor extends Actor with ActorLogging { // repository
     implicit val ec: ExecutionContext = context.dispatcher

  val patientDao = new PatientDAO()
  var patients = Set.empty[Patient]

  override def receive: Receive = {

      case GetPatients() =>
          val rootsender = sender()
          patientDao.getPatients().onComplete {
              case Success(value: Seq[Patient]) =>{
                  println("PAtients")
                  rootsender ! Patients(value)}
              case Failure(exp) => rootsender ! ActionPerformed(exp.getMessage)
          }

      case AddPatient(p: FullPatient) =>
          val rootsender = sender()
          patientDao.addPatient(p).onComplete {
              case Success(value) => rootsender ! value
              case Failure(exp) => rootsender ! ActionPerformed(exp.getMessage)
          }

      case GetbyID(id: Int) =>
          val rootsender = sender()
          patientDao.getPatient(id).onComplete {
              case Success(value) => rootsender ! value.headOption
              case Failure(exp) => rootsender ! ActionPerformed("Failed")
          }

      case DeletePatient(id: Int) =>
          val rootsender = sender()
          patientDao.deletePatient(id).onComplete {
              case Success(value: Try[Int]) => {
                  value match {
                      case Success(id: Int) => {
                          if (id > 0)
                              rootsender ! ActionPerformed("Deleted")
                          else
                              rootsender ! ActionPerformed("Failed")
                      }
                      case Failure(exp) =>
                          rootsender ! ActionPerformed("Failed")
                  }
              }
              case Failure(exp) => rootsender ! ActionPerformed("Failed")
          }

      case UpdatePatient(id: Int, p: Patient) => {
          val rootsender = sender()
          patientDao.updatePatient(id, p).onComplete {
              case Success(value: Try[Int]) => {
                  value match {
                      case Success(id) => {
                          if (id > 0) {
                              rootsender ! ActionPerformed("Updated")
                          }
                          else
                              rootsender ! ActionPerformed("Failed")
                      }
                      case Failure(exp) => ActionPerformed("Failed")
                  }
              }
              case Failure(exp) => rootsender ! ActionPerformed("Failed")
          }

      }
  }
}
