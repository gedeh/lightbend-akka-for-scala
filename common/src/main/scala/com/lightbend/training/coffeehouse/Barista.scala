package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import com.lightbend.training.coffeehouse.Barista.{CoffeePrepared, PrepareCoffee}

import scala.concurrent.duration.FiniteDuration

object Barista {
  case class PrepareCoffee(coffee: Coffee, guest: ActorRef)
  case class CoffeePrepared(coffee: Coffee, guest: ActorRef)

  def props(prepareCoffeeDuration: FiniteDuration): Props = Props(new Barista(prepareCoffeeDuration))
}

class Barista(prepareCoffeeDuration: FiniteDuration) extends Actor with ActorLogging {
  override def receive: Receive = {
    case PrepareCoffee(coffee, guest) =>
      log.info(s"Preparing coffee $coffee for ${guest.path.name}")
      busy(prepareCoffeeDuration)
      sender() ! CoffeePrepared(coffee, guest)
  }
}
