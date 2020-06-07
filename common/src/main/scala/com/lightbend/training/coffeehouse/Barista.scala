package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.lightbend.training.coffeehouse.Barista.{CoffeePrepared, PrepareCoffee}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import akka.actor.Timers
import akka.actor.Stash

object Barista {
  case class PrepareCoffee(coffee: Coffee, guest: ActorRef)
  case class CoffeePrepared(coffee: Coffee, guest: ActorRef)

  def props(prepareCoffeeDuration: FiniteDuration, accuracy: Int): Props = Props(new Barista(prepareCoffeeDuration, accuracy))
}

class Barista(prepareCoffeeDuration: FiniteDuration, accuracy: Int)
  extends Actor
  with ActorLogging
  with Timers
  with Stash {

  private def pickCoffee(coffee: Coffee): Coffee = if (Random.nextInt(100) < accuracy) coffee else Coffee.anyOther(coffee)

  override def receive: Receive = ready

  private def ready: Receive = {
    case PrepareCoffee(coffee , guest) =>
      val coffeeMade = pickCoffee(coffee)
      val message = CoffeePrepared(coffeeMade, guest)

      log.info(s"Preparing coffee $coffeeMade for guest ${guest.path.name}. Original order is $coffee")
      timers.startSingleTimer("coffee-prepared", message, prepareCoffeeDuration)
      context.become(busy(sender()))
  }

  private def busy(waiter: ActorRef): Receive = {
    case message: CoffeePrepared =>
      waiter ! message
      unstashAll()
      context.become(ready)
    case _ =>
      stash()
  }
}
