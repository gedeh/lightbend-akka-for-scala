package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging}
import akka.actor.Props

object CoffeeHouse {
    def props(): Props = Props(new CoffeeHouse)
}

class CoffeeHouse extends Actor with ActorLogging {

    log.info("CoffeHouse Open")

    override def receive: Receive = {
        case _ => {
            log.info("Coffee Brewing")
            sender() ! "Coffee Brewing"
        }
    }
}