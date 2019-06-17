package model

import AbstractClasses._

case class Patient (ID: Option[Int] = None, name: String, surname : String,
                    username: String = "+", password: String = "+", number: Long, stringCalendar: String) extends User {
    //val history : HashMap[Session, Description] = HashMap.empty
    //val cal: WeekCalenderP = WeekCalenderP();
    override def hashCode(): Int = {
        val prime = 31
        var result = 0
        result += prime * (if (name == null) 1 else name.hashCode())
        result += prime * (if (surname == null) 1 else surname.hashCode())
        result += prime * (if (username == null) 1 else username.hashCode())
        result += (prime * (if (password == null) 1 else password.hashCode()))
        super.hashCode() + result
    }
    def canEqual(cur: Any) = cur.isInstanceOf[Patient]
//    def toCalendar () : Unit = {
//        val list: Array[String] = this.stringCalendar.split("|")
//        for (i: String <- list) {
//            val curDay = Day(i(0).toInt)
//            val listofSessions = i.substring(2).split("&")
//            listofSessions.foreach(x => curDay.daySchedule :+ Session(x))
//            cal.schedule :+ curDay
//        }
//    }

    override def equals(obj: Any): Boolean = {
        if (canEqual(obj)&& obj.hashCode() == this.hashCode())
            true
        false
    }
}
case class FullPatient (name: String, surname: String, username: String, password: String, number: Long, stringCalendar: String)

