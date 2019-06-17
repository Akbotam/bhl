package dao


import model.{FullPatient, Patient}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future
import akka.stream.alpakka.slick.scaladsl._

class PatientDAO () {
    val connectionUrl = "jdbc:postgresql://localhost:5432/med_center?user=postgres&password=uljanek06"
    var allpatients = TableQuery[PatientTable]
    var db = Database.forURL(connectionUrl, driver = "org.postgresql.Driver")
    implicit val session = SlickSession.forConfig("slick-postgres")
    def getPatients(): Future[Seq[Patient]] = db.run(allpatients.result)

    def addPatient(p: FullPatient) = {

        val patient = Patient(None, p.name, p.surname, p.username, p.password, p.number, p.stringCalendar)
        val action =
            (allpatients returning allpatients.map(_.ID) += patient).map(id => patient.copy(ID = Some(id)))
            //(allpatients returning allpatients.map(_.patient_id) into ((pp, patient_id) => pp.copy(patient_id = Some(patient_id)))) += patient
        db.run(action)

    }
    def addPatientTest(p: Patient) = {
        val action = (allpatients returning allpatients += p)
        db.run(action)
    }
    def getPatient(id: Int) = db.run( allpatients.filter(_.ID === id).result)
    def updatePatient(id: Int , newP: Patient) = {

        val action = for { b <- allpatients if b.ID === id } yield (b.name, b.surname, b.username, b.password, b.number, b.stringCalendar)
        db.run(action.update(newP.name, newP.surname, newP.username, newP.password, newP.number, newP.stringCalendar).asTry)

    }
    def deletePatient(id: Int) = db.run(allpatients.filter(_.ID === id).delete.asTry)
}