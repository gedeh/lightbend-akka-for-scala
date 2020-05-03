package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit
import java.util.UUID.randomUUID
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.duration._

object CoffeeHouse {
    case class CreateGuest(favoriteCoffee: Coffee)

    def props(): Props = Props(new CoffeeHouse)
}

class CoffeeHouse extends Actor with ActorLogging {

    import CoffeeHouse._

    private val finishCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.guest.finish-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val prepareCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.barista.prepare-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val barista: ActorRef = createBarista()
    private val waiter: ActorRef = createWaiter()

    protected def createWaiter(): ActorRef = {
        context.actorOf(Waiter.props(barista), "waiter")
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
            createGuest(favoriteCoffee)
    }
}