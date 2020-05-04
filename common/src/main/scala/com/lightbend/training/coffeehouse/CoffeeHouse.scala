package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object CoffeeHouse {
    case class CreateGuest(favoriteCoffee: Coffee)
    case class ApproveCoffee(coffee: Coffee, guest: ActorRef)

    def props(caffeineLimit: Int): Props = Props(new CoffeeHouse(caffeineLimit))
}

class CoffeeHouse(caffeineLimit: Int) extends Actor with ActorLogging {

    import Barista._
    import CoffeeHouse._

    //noinspection ActorMutableStateInspection
    private var guestBook: Map[ActorRef, Int] = Map.empty.withDefaultValue(0)

    private val finishCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.guest.finish-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val prepareCoffeeDuration: FiniteDuration =
        context.system.settings.config.getDuration(
            "coffee-house.barista.prepare-coffee-duration",
            TimeUnit.SECONDS).seconds

    private val waiter: ActorRef = createWaiter(self)
    private val barista: ActorRef = createBarista()

    protected def createWaiter(coffeHouse: ActorRef): ActorRef = {
        context.actorOf(Waiter.props(coffeHouse), "waiter")
    }

    protected def createBarista(): ActorRef = {
        context.actorOf(Barista.props(prepareCoffeeDuration), "barista")
    }

    protected def createGuest(favoriteCoffee: Coffee): ActorRef = {
        val guest = context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration))
        context.watch(guest)
    }

    private def guestName(guest: ActorRef): String = {
        guest.path.name
    }

    override def preStart(): Unit = {
        log.debug("CoffeHouse Open")
    }

    override def receive: Receive = {
        case CreateGuest(favoriteCoffee) =>
            val guest = createGuest(favoriteCoffee)
            guestBook += guest -> 0
            log.info(s"Guest ${guestName(guest)} added to guest book. Active guests ${guestBook.size}")
        case ApproveCoffee(coffee, guest) if guestBook(guest) < caffeineLimit =>
            val newCount = guestBook(guest) + 1
            guestBook += guest -> newCount
            log.info(s"Guest ${guestName(guest)} caffeine count incremented to $newCount")
            barista.forward(PrepareCoffee(coffee, guest))
        case ApproveCoffee(_, guest) =>
            log.info(s"Sorry, ${guestName(guest)}, but you have reached your limit")
            context.stop(guest)
        case Terminated(guest) =>
            guestBook -= guest
            log.info(s"Thank you ${guestName(guest)}, for being our guest! Active guests ${guestBook.size}")
    }
}