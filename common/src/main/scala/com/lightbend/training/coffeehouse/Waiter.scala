package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Waiter {
  case class ServeCoffee(coffee: Coffee)
  case class CoffeeServed(coffee: Coffee)
  case class Complaint(coffee: Coffee)
  case object FrustratedException extends IllegalStateException

  def props(coffeeHouse: ActorRef, barista: ActorRef, maxComplaintCount: Int): Props =
    Props(new Waiter(coffeeHouse, barista, maxComplaintCount))
}

class Waiter(coffeeHouse: ActorRef, barista: ActorRef, maxComplaintCount: Int) extends Actor with ActorLogging {

  import Waiter._
  import Barista._

  //noinspection ActorMutableStateInspection
  private var complaintsReceived: Int = 0

  override def receive: Receive = {
    case ServeCoffee(coffee) =>
      log.info(s"Sending order coffee $coffee from guest ${sender().path.name} to coffee-house")
      coffeeHouse ! CoffeeHouse.ApproveCoffee(coffee, sender())
    case CoffeePrepared(coffee, guest) =>
      log.info(s"Serving coffee $coffee to guest ${guest.path.name}")
      guest ! CoffeeServed(coffee)
    case Complaint(coffee) if (complaintsReceived < maxComplaintCount) =>
      complaintsReceived += 1
      barista ! Barista.PrepareCoffee(coffee, sender())
    case Complaint(_) =>
      throw FrustratedException
  }
}
