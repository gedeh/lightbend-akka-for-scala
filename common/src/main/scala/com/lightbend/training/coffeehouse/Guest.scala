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

  var coffeeCount: Int = 0
  orderCoffee()

  override def receive: Receive = {
    case Waiter.CoffeeServed(coffee) =>
      coffeeCount += 1
      log.info(s"Enjoying my $coffeeCount yummy $coffee!")
      timers.startSingleTimer("coffee-finished", CoffeeFinished, finishCoffeeDuration)
    case Guest.CoffeeFinished =>
      log.info(s"Finished my latest coffee")
      orderCoffee()
  }

  private def orderCoffee(): Unit = waiter ! Waiter.ServeCoffee(favoriteCoffee)
}
