package dao

import model.Patient
import slick.jdbc.PostgresProfile.api._

class PatientTable(tag: Tag) extends Table[Patient](tag, "patients") {
    def ID = column[Int]("patient_id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def surname = column[String]("surname")
    def username= column[String]("login")
    def password = column[String]("password")
    def number = column[Long]("number")
    def stringCalendar = column[String]("stringCalendar")
    def * = (ID.?, name, surname, username, password, number, stringCalendar) <> (Patient.tupled, Patient.unapply)
    def idx = index("unique_id", ID, unique = true)
}