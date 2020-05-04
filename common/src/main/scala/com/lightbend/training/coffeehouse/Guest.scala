package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import com.lightbend.training.coffeehouse.Guest.{CaffeineException, CoffeeFinished}

import scala.concurrent.duration.FiniteDuration

object Guest {
  case class CoffeeFinished()
  case object CaffeineException extends IllegalStateException

  def props(
      waiter: ActorRef,
      favoriteCoffee: Coffee,
      finishCoffeeDuration: FiniteDuration,
      caffeineLimit: Int): Props =
    Props(new Guest(
      waiter,
      favoriteCoffee,
      finishCoffeeDuration,
      caffeineLimit))
}

class Guest(
    waiter: ActorRef,
    favoriteCoffee: Coffee,
    finishCoffeeDuration: FiniteDuration,
    caffeineLimit: Int)
  extends Actor
    with ActorLogging
    with Timers {

  var coffeeCount: Int = 0
  orderCoffee()

  override def receive: Receive = {
    case Waiter.CoffeeServed(`favoriteCoffee`) =>
      coffeeCount += 1
      log.info(s"Enjoying my #$coffeeCount $favoriteCoffee")
      timers.startSingleTimer("coffee-finished", CoffeeFinished, finishCoffeeDuration)
    case Waiter.CoffeeServed(coffee) =>
      log.info(s"Received wrong coffee. Expected a $favoriteCoffee but got a $coffee")
      waiter ! Waiter.Complaint(favoriteCoffee)
    case Guest.CoffeeFinished if coffeeCount <= caffeineLimit =>
      log.info(s"Finished my #$coffeeCount $favoriteCoffee")
      orderCoffee()
    case Guest.CoffeeFinished =>
      val diagnostic = s"I have too much caffeine (I drank $coffeeCount, limited to $caffeineLimit)"
      log.warning(diagnostic)
      throw CaffeineException
  }

  override def postStop(): Unit = {
    log.info("Well that's it then, good bye!")
    super.postStop()
  }

  private def orderCoffee(): Unit = {
    log.info(s"Ordering my #${coffeeCount + 1} $favoriteCoffee to waiter")
    waiter ! Waiter.ServeCoffee(favoriteCoffee)
  }

}
