package actors

import akka.actor.{Actor, ActorLogging, Props}
import models.{Buyer, Buyers, Drug, Drugs, Pharmacy => PharmacyModel}
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

import scala.concurrent.Await
import scala.concurrent.duration._



class DrugsTable(tag: Tag) extends Table[Drug](tag, "DRUGS"){
  def drugId: Rep[Int] = column[Int]("DRUG_ID", O.PrimaryKey, O.AutoInc)
//  def drugId: Rep[Int] = column[Int]("DRUG_ID", O.PrimaryKey)
  def drugName: Rep[String] = column[String]("DRUG_NAME")
  def drugDescription: Rep[String] = column[String]("DRUG_DESCRIPTION")
  def drugAmount: Rep[Int] = column[Int]("DRUG_AMOUNT", O.Default(10))

  def * : ProvenShape[Drug] = (drugId.?, drugName, drugDescription, drugAmount) <> (Drug.tupled, Drug.unapply)
}

class BuyersTable(tag:Tag) extends Table[Buyer](tag, "BUYERS"){
  def buyerId= column[Int]("BUYER_ID", O.PrimaryKey, O.AutoInc)
  def buyerName = column[String]("BUYER_NAME")
  def buyerMoney = column[Int]("BUYER_MONEY", O.Default(1000))

  def * : ProvenShape[Buyer] = (buyerId.?, buyerName, buyerMoney) <> (Buyer.tupled, Buyer.unapply)
}


object Pharmacy {

  def props(db: MySQLProfile.backend.Database) = Props(new Pharmacy(db))

  case object GetDrugs
  case class CreateDrug(name:String, description: String, amount: Int)
  case class UpdateDrug(newDrug: Drug)
  case class GetDrug(id:Int)
  case class DeleteDrug(id: Int)

  case object GetBuyers
  case class CreateBuyer(name:String, money: Int)
  case class UpdateBuyer(newBuyer: Buyer)
  case class GetBuyer(id: Int)
  case class DeleteBuyer(id: Int)

}

class Pharmacy(db: MySQLProfile.backend.Database) extends Actor with ActorLogging{
  import Pharmacy._

  val drugsTable = TableQuery[DrugsTable]
  val buyersTable = TableQuery[BuyersTable]

  val drugsList: Seq[Drug] = Await.result(db.run(drugsTable.result), 5.seconds)

  override def receive: Receive = {
    case CreateDrug(newName, newDescription, newAmount) =>
      log.info(s"POST CreateDrug message got ==> $newName")
      sender() ! db.run(
        drugsTable += Drug(name=newName, description=newDescription, amount=newAmount)
      )
    case GetDrugs =>
      log.info(s"GET GetDrugs")
      sender() ! Drugs(Await.result(db.run(drugsTable.result), 10.seconds))

    case GetDrug(drugId) => {
      log.info(s"Received GetDrug with drugId: ${drugId}.")
//      val a =db.run(drugsTable.filter(_.drugId === drugId).result.headOption)
        sender() ! db.run(drugsTable.filter(_.drugId === drugId).result.head)
    }
    case UpdateDrug(newDrug) =>
      log.info(s"\n Name: ${newDrug.name} & Description: ${newDrug.description} & amount: ${newDrug.amount} & newDrugId: ${newDrug.id} \n\n\n\n\n}")
      db.run (
        drugsTable.filter(_.drugId === newDrug.id.get).map(d => (d.drugName, d.drugDescription, d.drugAmount))
          .update(newDrug.name, newDrug.description, newDrug.amount)
        )
      sender() ! db.run(drugsTable.filter(_.drugId === newDrug.id.get).result.head)

    case DeleteDrug(drugId) => {
      sender() ! db.run(drugsTable.filter(_.drugId === drugId).delete)
    }


    /* ---------------------------- Buyer -----------------------------*/
    case CreateBuyer(newName, newMoney) =>
      log.info(s"POST Create Buyer message got ==> $newName with money: $newMoney")
      sender() ! db.run(
        buyersTable += Buyer(name=newName, money=newMoney)
      )
    case GetBuyers =>
      log.info(s"GET Get Buyers.")
      sender() ! Buyers(Await.result(db.run(buyersTable.result), 10.seconds))

    case GetBuyer(buyerId) => {
      log.info(s"Received Get Buyer with id: ${buyerId}.")
      sender() ! db.run(buyersTable.filter(_.buyerId === buyerId).result.head)
    }
    case UpdateBuyer(newBuyer) =>
      db.run (
        buyersTable.filter(_.buyerId === newBuyer.id.get).map(b => (b.buyerName, b.buyerMoney))
          .update(newBuyer.name, newBuyer.money)
      )
      sender() ! db.run(buyersTable.filter(_.buyerId === newBuyer.id.get).result.head)

    case DeleteBuyer(buyerId) => {
      sender() ! db.run(buyersTable.filter(_.buyerId ===  buyerId).delete)
    }
  }
}


