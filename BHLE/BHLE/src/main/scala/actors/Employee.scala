package actors

import abstractClasses.{Message, User}
import actors.Employee.{CancelSession, GetData}
import akka.actor.{Actor, ActorLogging, Props}
import model.{EmployeeModel, Session}

import scala.collection.mutable

object Employee {
    case class RegisterSession(id: String, time: Int)
    case class CancelSession (id: String, time: Int)
    case object GetData
    def props(id: String, name: String, username : String, password: String, category: String) =
        Props(new Employee(id, name, username, password, category))

}
case class Employee (   ID: String, name: String,
                        username: String = "+", password: String = "+",
                        category: String,
                        schedule: mutable.HashMap[Int, Boolean] = mutable.HashMap.empty[Int, Boolean])
                        extends User with Actor with ActorLogging {

    //var schedule =

    log.info("Employee with id: {} created", ID)

    override def receive: Receive = {
        case msg: Employee.RegisterSession => {
            println("Employee find matches")
            if (schedule.contains(msg.time) && schedule(msg.time) == true)
            {
                sender() ! Message("TIME is not free")
            }
            else {
                schedule(msg.time) = false
                println("Time is free!")
                sender() !  Session(msg.id, ID, msg.time)
            }
        }

        case GetData => {
            println(name)
            sender() ! EmployeeModel(ID, name, username, password, category)
        }
        case msg: CancelSession => {
            if (schedule.contains(msg.time))
                schedule(msg.time) = true
            sender() ! Message ("CANCELED")
        }

        case any: Any =>
            log.warning("Unexpected: {}", any)
    }
}