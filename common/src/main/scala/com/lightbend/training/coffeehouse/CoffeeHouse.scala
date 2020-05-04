package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}

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
            case CaffeineException => {
                log.info("Got a guest with too many caffeine, stopping them")
                Stop
            }
            case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
        }
        OneForOneStrategy()(decider.orElse(super.supervisorStrategy.decider))
    }

    private val finishCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.guest.finish-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val prepareCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.barista.prepare-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val waiter: ActorRef = createWaiter(self)
    private val barista: ActorRef = createBarista()

    protected def createWaiter(coffeHouse: ActorRef): ActorRef = {
        context.actorOf(Waiter.props(coffeHouse), "waiter")
    }

    protected def createBarista(): ActorRef = {
        context.actorOf(Barista.props(prepareCoffeeDuration), "barista")
    }

    protected def createGuest(favoriteCoffee: Coffee, caffeineLimit: Int): ActorRef = {
        val guest = context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration, caffeineLimit))
        log.debug(s"Created guest ${actorName(guest)}, favorite coffee $favoriteCoffee, caffeine limit: $caffeineLimit")
        context.watch(guest)
    }

    private def actorName(guest: ActorRef): String = {
        guest.path.name
    }

    override def preStart(): Unit = {
        log.debug("CoffeHouse Open")
    }

    override def receive: Receive = {
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