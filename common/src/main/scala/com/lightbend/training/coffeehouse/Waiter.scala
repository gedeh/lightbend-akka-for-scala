package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.lightbend.training.coffeehouse.Waiter.CoffeeServed

object Waiter {
  case class ServeCoffee(coffee: Coffee) // parameter to function
  case class CoffeeServed(coffee: Coffee) // return type of the function

  def props(barista: ActorRef): Props = Props(new Waiter(barista))
}

class Waiter(barista: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Waiter.ServeCoffee(coffee) =>
      log.info(s"Sending order coffee $coffee from ${sender().path.name} to barista")
      barista ! Barista.PrepareCoffee(coffee, sender())
    case Barista.CoffeePrepared(coffee, guest) =>
      log.info(s"Serving coffee $coffee to ${guest.path.name}")
      guest ! CoffeeServed(coffee)
  }
}
