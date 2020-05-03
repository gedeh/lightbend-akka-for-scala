package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import com.lightbend.training.coffeehouse.Guest.CoffeeFinished

import scala.concurrent.duration.FiniteDuration

object Guest {
  case class CoffeeFinished()

  def props(
      waiter: ActorRef,
      favoriteCoffee: Coffee,
      finishCoffeeDuration: FiniteDuration): Props =
    Props(new Guest(waiter, favoriteCoffee, finishCoffeeDuration))
}

class Guest(waiter: ActorRef, favoriteCoffee: Coffee, finishCoffeeDuration: FiniteDuration)
  extends Actor
    with ActorLogging
    with Timers {

  val maxCoffee = 10
  var coffeeCount: Int = 0
  orderCoffee()

  override def receive: Receive = {
    case Waiter.CoffeeServed(coffee) =>
      coffeeCount += 1
      log.info(s"Enjoying my #$coffeeCount $coffee!")
      timers.startSingleTimer("coffee-finished", CoffeeFinished, finishCoffeeDuration)
    case Guest.CoffeeFinished =>
      log.info(s"Finished my #$coffeeCount $favoriteCoffee")
      if (coffeeCount != maxCoffee) orderCoffee()
  }

  private def orderCoffee(): Unit = {
    log.info(s"Order #${coffeeCount + 1} $favoriteCoffee to waiter")
    waiter ! Waiter.ServeCoffee(favoriteCoffee)
  }

}
