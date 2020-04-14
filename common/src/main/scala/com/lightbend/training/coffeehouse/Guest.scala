package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Guest {
  case class CoffeeFinished()

  def props(waiter: ActorRef, favoriteCoffee: Coffee): Props = Props(new Guest(waiter, favoriteCoffee))
}

class Guest(waiter: ActorRef, favoriteCoffee: Coffee) extends Actor with ActorLogging {
  var coffeeCount: Int = 0

  override def receive: Receive = {
    case Waiter.CoffeeServed(coffee) =>
      coffeeCount += 1
      log.info(s"Enjoying my $coffeeCount yummy $coffee!")
      throw new NullPointerException
    case Guest.CoffeeFinished =>
      log.info(s"Finished my latest coffee")
      waiter ! Waiter.ServeCoffee(favoriteCoffee)
  }
}
