package actors

import abstractClasses.{Message, User}
import actors.Administrator.CreateCustomer
import actors.Customer.FailedRegisterSession
import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import model.{CustomerModel, EmployeeModel, Session}

import scala.concurrent.duration._

object Administrator {
    case class CreateCustomer(customer: CustomerModel)

    case class CreateEmployee(employee: EmployeeModel)

    case class RegisterSession(s: Session)

    case class CancelSession(s: Session)

    case class Response(message: String)

    case class GetEmployees()

    case class Employees(seq: Seq[EmployeeModel])

    case class GetEmployee(id: String)

    case class ApproveSession (s: Session, replyTo: ActorRef)

    case class GetEmployeeWithNameId(name:String, id: String)

    def props(ID: String, name: String, username: String, password: String, number: Long) = Props(new Administrator(ID, name, username,
        password, number))

}
case class Administrator(ID: String, name: String, username: String = "+", password: String = "+", number: Long) extends User
                        with Actor with ActorLogging {

    context.setReceiveTimeout(3.seconds)

    import Administrator._

    var employees = Map.empty[String, ActorRef] //scala.collection.mutable.Map.empty[String, ActorRef] //
    var customers = Map.empty[String, ActorRef]
    var sessions = Map.empty[String, Int]

    override def receive: Receive = {
        case GetEmployeeWithNameId(name, id) => {
            employees.get(id)  match {
                case Some(employee) =>
                    employee ! Employee.GetData
                    context.become(waitingEmployeeResponse(sender()))
                case None => sender() ! Message(s"Cannot get employee with id $id")
            }
        }

        case CreateEmployee(cur: EmployeeModel) =>
            {
                val employee: ActorRef = context.actorOf(Employee.props(cur.ID, cur.name, cur.username, cur.password, cur.category), cur.ID)
                println(s"CreateEmployee with id ${cur.ID}")
                employees = employees + (cur.ID -> employee)
                println(sender().path.name)
                sender() ! Message("OK")
            }
        case CreateCustomer(cur: CustomerModel) => {
            println(s"CreateCustomer with id ${cur.ID}")
            val customer: ActorRef = context.actorOf(Customer.props(cur.ID, cur.name,cur.username, cur.password), cur.ID)
            customers = customers + (cur.ID -> customer)
            sender() ! Message("OK")
        }
        case RegisterSession(s) => {
            println(s"Register customer with id  ${s.customerID} to employee with id ${s.employeeID} on time ${s.time}")
            if (employees.contains(s.employeeID) && customers.contains(s.customerID)) {
                println(employees(s.employeeID).path.name)
                //sessions = sessions + (s.employeeID -> s.time)
                val temp: ActorRef = employees(s.employeeID)
                log.info("Sending Register to employee: {}", temp)
                temp ! Employee.RegisterSession(s.customerID, s.time)
                context.become(waitingSessionResponce(customers(s.customerID), s, sender()))
                //customers(s.customerID) ! Customer.RegisterSession(s.employeeID, s.time)
            }
            else
                sender() ! Message("Invalid ids of users")
        }
        case CancelSession(s: Session) =>  {
           if (employees.contains(s.employeeID) && customers.contains(s.customerID))  {
               sessions = sessions - (s.employeeID)
               employees(s.employeeID) ! Employee.CancelSession(s.employeeID, s.time)
               customers(s.customerID) ! Customer.CancelSesion(s.customerID, s.time)
           }

        }
        case GetEmployees() => {
            println("GET EMPLOYEES")
            println(employees.size)
            employees.values.foreach((employee: ActorRef) =>  employee ! Employee.GetData)

            context.become(waitingResponses(employees.size, sender(), Seq.empty[EmployeeModel]))
        }
        case GetEmployee(id) => {
            employees.get(id)  match {
                case Some(employee) =>
                    employee ! Employee.GetData
                    context.become(waitingEmployeeResponse(sender()))
                case None => sender() ! Message(s"Cannot get employee with id $id")
            }
        }
        case Message(msg) => {
            context.parent ! Message(msg)
        }
        case ApproveSession (s, replyTo) => {
            println(context.parent.path.name)
            sessions = sessions + (s.employeeID -> s.time)
            replyTo ! Message("SUCCESS REGISTRATION")
        }
    }
    def waitingResponses(responsesLeft: Int, replyTo: ActorRef, employees: Seq[EmployeeModel]): Receive = {
        case employee: EmployeeModel => {
            println(s"Received BookModel with name: ${employee.name}. Responses left: $responsesLeft")
            if (responsesLeft - 1 == 0) {
                log.info("All responses received, replying to initial request.")
                replyTo ! Administrator.Employees(employees :+ employee)
                context.become(receive)
            }
            else context.become(waitingResponses(responsesLeft - 1, replyTo, employees = employees :+ employee))
        }

    }
    def waitingEmployeeResponse(replyTo: ActorRef): Receive = {
        case employee: EmployeeModel =>
            replyTo ! employee
            context.become(receive)

        case ReceiveTimeout =>
            replyTo ! Message("Timeout when looking for book.")
    }
    def waitingSessionResponce (to: ActorRef, s: Session, replyTo: ActorRef) : Receive = {
        case s: Session =>
            println("Receive accept from employee")
            println(to.path.name)
            to ! Customer.RegisterSession(s.employeeID, s.time, replyTo)
            context.become(receive)
        case msg: Message =>
            to ! FailedRegisterSession("FAIL")
            context.become(receive)
    }
}


