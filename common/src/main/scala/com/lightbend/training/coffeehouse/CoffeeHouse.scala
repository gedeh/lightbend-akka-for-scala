package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object CoffeeHouse {
    case class CreateGuest(favoriteCoffee: Coffee)

    def props(): Props = Props(new CoffeeHouse)
}

class CoffeeHouse extends Actor with ActorLogging {
    import CoffeeHouse._

    log.info("CoffeHouse Open")
    val waiter: ActorRef = createWaiter()

    protected def createWaiter(): ActorRef = context.actorOf(Waiter.props(), "waiter")
    protected def createGuest(favoriteCoffee: Coffee): ActorRef = {
        context.actorOf(Guest.props(waiter, favoriteCoffee))
    }

    override def receive: Receive = {
        case CreateGuest(favoriteCoffee) =>
            log.info("Create Guest")
            createGuest(favoriteCoffee)
    }
}