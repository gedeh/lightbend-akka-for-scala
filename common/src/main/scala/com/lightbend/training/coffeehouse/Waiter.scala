package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, Props}

object Waiter {
  case class ServeCoffee(coffee: Coffee) // parameter to function
  case class CoffeeServed(coffee: Coffee) // return type of the function

  def props(): Props = Props(new Waiter)
}

class Waiter extends Actor with ActorLogging {
  override def receive: Receive = {
    case Waiter.ServeCoffee(coffee) =>
      log.info(s"Serving coffee $coffee")
      sender() ! Waiter.CoffeeServed(coffee)
  }
}
