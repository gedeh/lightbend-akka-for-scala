package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Waiter {
  case class ServeCoffee(coffee: Coffee) // parameter to function
  case class CoffeeServed(coffee: Coffee) // return type of the function

  def props(barista: ActorRef): Props = Props(new Waiter(barista))
}

class Waiter(barista: ActorRef) extends Actor with ActorLogging {

  import Waiter._
  import Barista._

  override def receive: Receive = {
    case ServeCoffee(coffee) =>
      log.info(s"Sending order coffee $coffee from ${sender().path.name} to barista")
      barista ! CoffeeHouse.ApproveCoffee(coffee, sender())
    case CoffeePrepared(coffee, guest) =>
      log.info(s"Serving coffee $coffee to ${guest.path.name}")
      guest ! CoffeeServed(coffee)
  }
}
