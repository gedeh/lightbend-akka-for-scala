package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.lightbend.training.coffeehouse.Barista.{CoffeePrepared, PrepareCoffee}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Barista {
  case class PrepareCoffee(coffee: Coffee, guest: ActorRef)
  case class CoffeePrepared(coffee: Coffee, guest: ActorRef)

  def props(prepareCoffeeDuration: FiniteDuration, accuracy: Int): Props = Props(new Barista(prepareCoffeeDuration, accuracy))
}

class Barista(prepareCoffeeDuration: FiniteDuration, accuracy: Int) extends Actor with ActorLogging {
  override def receive: Receive = {
    case PrepareCoffee(coffee, guest) =>
      val coffeeMade = if (Random.nextInt(100) < accuracy) coffee else Coffee.anyOther(coffee)
      log.info(s"Preparing coffee $coffeeMade for guest ${guest.path.name}. Original order is $coffee")
      busy(prepareCoffeeDuration)
      sender() ! CoffeePrepared(coffeeMade, guest)
  }
}
