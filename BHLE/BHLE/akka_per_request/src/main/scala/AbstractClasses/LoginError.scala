package AbstractClasses

case class LoginError (ID: Option[Int] = None, name:String =  "", surname: String = "", username: String = "",
                       password: String ="",number: Long =0, stringCalendar: String="") extends User {
}
