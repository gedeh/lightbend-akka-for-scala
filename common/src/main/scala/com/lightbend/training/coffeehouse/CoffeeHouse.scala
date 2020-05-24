package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.event.LoggingReceive
import akka.routing.FromConfig
import com.lightbend.training.coffeehouse.Waiter.FrustratedException

import scala.concurrent.duration._

object CoffeeHouse {
    case class CreateGuest(favoriteCoffee: Coffee, caffeineLimit: Int)
    case class ApproveCoffee(coffee: Coffee, guest: ActorRef)

    def props(caffeineLimit: Int): Props = Props(new CoffeeHouse(caffeineLimit))
}

class CoffeeHouse(caffeineLimit: Int) extends Actor with ActorLogging {

    import Barista._
    import Guest._
    import CoffeeHouse._

    //noinspection ActorMutableStateInspection
    private var guestBook: Map[ActorRef, Int] = Map.empty.withDefaultValue(0)

    override val supervisorStrategy = {
        val decider: SupervisorStrategy.Decider = {
            case CaffeineException =>
                log.info("Got a guest with too many caffeine, stopping them")
                SupervisorStrategy.Stop
            case FrustratedException(coffee, guest) =>
                log.info("Got a waiter frustrated with too many complaints, restarting them")
                barista.forward(Barista.PrepareCoffee(coffee, guest))
                SupervisorStrategy.Restart
            case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => SupervisorStrategy.Escalate)
        }
        OneForOneStrategy()(decider.orElse(super.supervisorStrategy.decider))
    }

    private val waiterMaxComplaintCount: Int =
        context.system.settings.config.getInt("coffee-house.waiter.max-complaint-count")

    private val guestFinishCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.guest.finish-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val baristaAccuracy: Int =
        context.system.settings.config.getInt("coffee-house.barista.accuracy")

    private val baristaPrepareCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.barista.prepare-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val barista: ActorRef = createBarista()
    private val waiter: ActorRef = createWaiter()

    protected def createWaiter(): ActorRef = {
        context.actorOf(FromConfig.props(Waiter.props(self, barista, waiterMaxComplaintCount)), "waiter")
    }

    protected def createBarista(): ActorRef = {
        context.actorOf(FromConfig.props(Barista.props(baristaPrepareCoffeeDuration, baristaAccuracy)), "barista")
    }

    protected def createGuest(favoriteCoffee: Coffee, caffeineLimit: Int): ActorRef = {
        val guest = context.actorOf(Guest.props(waiter, favoriteCoffee, guestFinishCoffeeDuration, caffeineLimit))
        log.debug(s"Created guest ${actorName(guest)}, favorite coffee $favoriteCoffee, caffeine limit: $caffeineLimit")
        context.watch(guest)
    }

    private def actorName(guest: ActorRef): String = {
        guest.path.name
    }

    override def preStart(): Unit = {
        log.debug("CoffeHouse Open")
    }

    override def receive: Receive = LoggingReceive {
        case CreateGuest(favoriteCoffee, caffeineLimit) =>
            val guest = createGuest(favoriteCoffee, caffeineLimit)
            guestBook += guest -> 0
            log.info(s"Guest ${actorName(guest)} added to guest book. Active guests ${guestBook.size}")
        case ApproveCoffee(coffee, guest) if guestBook(guest) < caffeineLimit =>
            val newCount = guestBook(guest) + 1
            guestBook += guest -> newCount
            log.info(s"Received coffee order from guest ${actorName(guest)}. Guest caffeine count incremented to $newCount")
            barista.forward(PrepareCoffee(coffee, guest))
        case ApproveCoffee(_, guest) =>
            log.info(s"Received coffee order from guest ${actorName(guest)}. Sorry, but you have reached your limit")
            context.stop(guest)
        case Terminated(guest) =>
            guestBook -= guest
            log.info(s"Thank you ${actorName(guest)}, for being our guest! Active guests ${guestBook.size}")
    }
}