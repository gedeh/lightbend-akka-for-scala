package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit
import java.util.UUID.randomUUID
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.duration._

object CoffeeHouse {
    case class CreateGuest(favoriteCoffee: Coffee)
    case class ApproveCoffee(coffee: Coffee, guest: ActorRef)

    def props(caffeineLimit: Int): Props = Props(new CoffeeHouse(caffeineLimit))
}

class CoffeeHouse(caffeineLimit: Int) extends Actor with ActorLogging {

    import Barista._
    import CoffeeHouse._

    private var guestCaffeineIntake: Map[ActorRef, Int] = Map.empty.withDefaultValue(0)

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

    protected def createGuest(favoriteCoffee: Coffee): ActorRef = {
        context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration), s"guest-$randomUUID")
    }

    override def preStart(): Unit = {
        log.debug("CoffeHouse Open")
    }

    override def receive: Receive = {
        case CreateGuest(favoriteCoffee) =>
            log.info("Create Guest")
            val guest = createGuest(favoriteCoffee)
            log.info(s"Guest ${guest.path.name} added to guest book")
            guestCaffeineIntake += guest -> 0
        case ApproveCoffee(coffee, guest) if guestCaffeineIntake(guest) < caffeineLimit =>
            guestCaffeineIntake += guest -> (guestCaffeineIntake(guest) + 1)
            log.info(s"Guest ${guest.path.name} caffeine count incremented")
            barista.forward(PrepareCoffee(coffee, guest))
        case ApproveCoffee(_, guest) =>
            log.info(s"Sorry, ${guest.path.name}, but you have reached your limit")
            context.stop(guest)
    }
}