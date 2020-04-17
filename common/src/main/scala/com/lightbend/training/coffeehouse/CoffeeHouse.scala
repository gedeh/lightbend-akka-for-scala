package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.duration._

object CoffeeHouse {
    case class CreateGuest(favoriteCoffee: Coffee)

    def props(): Props = Props(new CoffeeHouse)
}

class CoffeeHouse extends Actor with ActorLogging {
    import CoffeeHouse._

    val waiter: ActorRef = createWaiter()

    private val finishCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.guest.finish-coffee-duration",
            TimeUnit.SECONDS).seconds

    protected def createWaiter(): ActorRef = {
        context.actorOf(Waiter.props(), "waiter")
    }

    protected def createGuest(favoriteCoffee: Coffee): ActorRef = {
        context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration))
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