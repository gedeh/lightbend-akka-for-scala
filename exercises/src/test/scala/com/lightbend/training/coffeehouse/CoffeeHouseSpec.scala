/**
 * Copyright © 2014 - 2020 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */

package com.lightbend.training.coffeehouse

import akka.actor.{ActorRef, Props}
import akka.testkit.{EventFilter, TestActorRef, TestProbe}

import scala.concurrent.duration.DurationInt

class CoffeeHouseSpec extends BaseAkkaSpec {

  "Creating CoffeeHouse" should {
    "result in logging a status message at debug" in {
      EventFilter.debug(pattern = ".*[Oo]pen.*", occurrences = 1) intercept {
        system.actorOf(CoffeeHouse.props(Int.MaxValue))
      }
    }
    "result in creating a child actor with the name 'barista'" in {
      system.actorOf(CoffeeHouse.props(Int.MaxValue), "create-barista")
      TestProbe().expectActor("/user/create-barista/barista")
    }
    "result in creating a child actor with the name 'waiter'" in {
      system.actorOf(CoffeeHouse.props(Int.MaxValue), "create-waiter")
      TestProbe().expectActor("/user/create-waiter/waiter")
    }
  }

  "Sending CreateGuest to CoffeeHouse" should {
    "result in creating a Guest" in {
      val coffeeHouse = system.actorOf(CoffeeHouse.props(Int.MaxValue), "create-guest")
      coffeeHouse ! CoffeeHouse.CreateGuest(Coffee.Akkaccino)
      TestProbe().expectActor("/user/create-guest/guest-*")
    }
    "result in logging status guest added to guest book" in {
      val coffeeHouse = system.actorOf(CoffeeHouse.props(Int.MaxValue), "add-to-guest-book")
      EventFilter.info(source = coffeeHouse.path.toString, pattern = ".*added to guest book.*", occurrences = 1) intercept {
        coffeeHouse ! CoffeeHouse.CreateGuest(Coffee.Akkaccino)
      }
    }
  }

  "Sending ApproveCoffee to CoffeeHouse" should {
    "result in forwarding PrepareCoffee to Barista if caffeineLimit not yet reached" in {
      val barista = TestProbe()
      val coffeeHouse =
        TestActorRef(new CoffeeHouse(Int.MaxValue) {
          override def createBarista() = barista.ref
        })
      coffeeHouse ! CoffeeHouse.ApproveCoffee(Coffee.Akkaccino, system.deadLetters)
      barista.expectMsg(Barista.PrepareCoffee(Coffee.Akkaccino, system.deadLetters))
    }
    "result in Barista sending a CoffeePrepared to Waiter if caffeineLimit not yet reached" in {
      val dummyGuest = TestProbe().ref // Just there to get an ActorRef...
      val dummyWaiter = TestProbe()
      val coffeeHouse = TestActorRef(new CoffeeHouse(Int.MaxValue) {
        override def createBarista(): ActorRef = context.actorOf(Barista.props(0.seconds), "barista")
      })
      coffeeHouse.tell(CoffeeHouse.ApproveCoffee(Coffee.Akkaccino, dummyGuest),dummyWaiter.ref)
      dummyWaiter.expectMsg(Barista.CoffeePrepared(Coffee.Akkaccino, dummyGuest))
    }
    "result in logging status guest caffeine count incremented" in {
      val dummyGuest = TestProbe()
      val coffeeHouse = system.actorOf(Props(new CoffeeHouse(Int.MaxValue) {
        override protected def createGuest(favoriteCoffee: Coffee): ActorRef = dummyGuest.ref
      }), "caffeine-count-incremented-guest-book")

      EventFilter.info(source = coffeeHouse.path.toString, pattern = ".*caffeine count incremented.*", occurrences = 1) intercept {
        coffeeHouse ! CoffeeHouse.CreateGuest(Coffee.Akkaccino)
        coffeeHouse ! CoffeeHouse.ApproveCoffee(Coffee.Akkaccino, dummyGuest.ref)
      }
    }
    "result in logging a status message at info if caffeineLimit reached" in {
      val coffeeHouse = system.actorOf(CoffeeHouse.props(0))
      EventFilter.info(source = coffeeHouse.path.toString, pattern = ".*[Ss]orry.*", occurrences = 1) intercept {
        val guest = TestProbe().ref
        coffeeHouse ! CoffeeHouse.ApproveCoffee(Coffee.Akkaccino, guest)
      }
    }
    "result in stopping the Guest if caffeineLimit reached" in {
      val probe = TestProbe()
      val guest = TestProbe().ref
      probe.watch(guest)
      val coffeeHouse = system.actorOf(CoffeeHouse.props(0))
      coffeeHouse ! CoffeeHouse.ApproveCoffee(Coffee.Akkaccino, guest)
      probe.expectTerminated(guest)
    }
  }
}
