import actors.{BuyersTable, DrugsTable, Pharmacy}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import models.{Drug, Drugs, Response, Buyer, Buyers}
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Boot extends App with JsonSupport {
  val log = LoggerFactory.getLogger("Boot")

  //needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  //needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(100.seconds)

  val db: MySQLProfile.backend.Database = Database.forConfig("mysql")

  val drugs = TableQuery[DrugsTable]
  val buyers = TableQuery[BuyersTable]


  val pharmacy = system.actorOf(Pharmacy.props(db), "pharmacy")

  val route =
    pathPrefix("pharmacy") {
      path("drugs") {
        post {
          entity(as[Drug]) { drug =>
            complete {
              (pharmacy ? Pharmacy.CreateDrug(drug.name, drug.description, drug.amount)).mapTo[Future[Int]].flatten.map(x => Response(x))
            }
          }
        } ~
          get {
            complete {
              (pharmacy ? Pharmacy.GetDrugs).mapTo[Drugs]
            }
          }
      } ~
        path("drugs" / IntNumber) { id =>
          get {
            complete {
              (pharmacy ? Pharmacy.GetDrug(id))
                .mapTo[Future[Drug]].flatten.map(x => x)
            }
          } ~
            delete {
              complete {
                (pharmacy ? Pharmacy.DeleteDrug(id))
                  .mapTo[Future[Int]].flatten.map(x => Response(x))
              }
            }
        } ~
        path("drug") {
          put {
            entity(as[Drug]) { drug =>
              complete {
                val newDrug = Drug(drug.id, drug.name, drug.description, drug.amount)
                (pharmacy ? Pharmacy.UpdateDrug(newDrug)).mapTo[Future[Drug]].flatten.map(x => x)
              }
            }
          }
        }

      path("buyers") {
        post {
          entity(as[Buyer]) { buyer =>
            complete {
              (pharmacy ? Pharmacy.CreateBuyer(buyer.name, buyer.money)).mapTo[Future[Int]].flatten.map(x => Response(x))
            }
          }
        } ~
          get {
            complete {
              (pharmacy ? Pharmacy.GetBuyers).mapTo[Buyers]
            }
          }
      } ~
        path("buyers" / IntNumber) { id =>
          get {
            complete {
              (pharmacy ? Pharmacy.GetBuyer(id))
                .mapTo[Future[Buyer]].flatten.map(x => x)
            }
          } ~
            delete {
              complete {
                (pharmacy ? Pharmacy.DeleteBuyer(id))
                  .mapTo[Future[Int]].flatten.map(x => Response(x))
              }
            }
        } ~
        path("buyer") {
          put {
            entity(as[Buyer]) { buyer =>
              complete {
                val newBuyer = Buyer(buyer.id, buyer.name, buyer.money)
                (pharmacy ? Pharmacy.UpdateBuyer(newBuyer)).mapTo[Future[Buyer]].flatten.map(x => x)
              }
            }
          }
        }
    }

  val config = ConfigFactory.load()

  val shouldCreate = config.getBoolean("create-schema")

  if (!shouldCreate) {
    try {
      Await.result(db.run(DBIO.seq(
        drugs.schema.create,
        buyers.schema.create,

        drugs.result.map(println),
        buyers.result.map(println),
      )), Duration.Inf)
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  log.info("Listening on port 8080...")

}
