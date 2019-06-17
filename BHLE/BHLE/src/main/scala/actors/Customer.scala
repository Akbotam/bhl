package actors

import abstractClasses.{Message, User}
import actors.Customer.{CancelSesion, FailedRegisterSession, RegisterSession}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import model.Session

import scala.collection.mutable


object Customer {
    case class RegisterSession (id: String,time: Int, replyTo: ActorRef)
    case class CancelSesion(id: String, time: Int )
    case class FailedRegisterSession(msg: String)
    def props(id: String, name: String, surname: String, password: String) = Props(new Customer(id, name, surname, password))

}
case class Customer (   ID: String, name: String,
                        username: String = "+", password: String = "+",
                        sessions: mutable.HashMap[Int, String] = mutable.HashMap.empty) extends User with Actor with ActorLogging{

    override def receive: Receive = {

        case msg: Customer.RegisterSession =>
            {
                if (sessions.contains(msg.time) && sessions(msg.time) != "")
                    {
                        println("CUSTOMER CANNOT REGISTER THE SESSION!")
                        sender() ! Message("CUSTOMER CANNOT REGISTER THE SESSION!")
                    }
                else {
                    println(sender().path.name)
                    sessions(msg.time) = msg.id
                    sender() ! Administrator.ApproveSession(Session(ID, msg.id, msg.time), msg.replyTo)
                }

            }
        case msg: FailedRegisterSession => {
            sender() ! Message(msg.msg)

        }

        case CancelSesion(id, time) =>
            {
                if (sessions.contains(time))
                    sessions(time) = ""
                sender() ! Message ("CANCEL SESSION")
            }

    }
}
