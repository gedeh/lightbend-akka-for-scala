/**
 * Copyright © 2014 - 2020 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */

package com.lightbend.training.coffeehouse

import akka.testkit.TestProbe
import scala.concurrent.duration.DurationInt

class BaristaSpec extends BaseAkkaSpec {

  "Sending PrepareCoffee to Barista" should {
    "result in sending a CoffeePrepared response after prepareCoffeeDuration" in {
      val sender = TestProbe()
      implicit val ref = sender.ref
      val barista = testActorSystem.actorOf(Barista.props(100 milliseconds))
      sender.within(50 milliseconds, 1000 milliseconds) { // busy is inaccurate, so we relax the timing constraints.
        barista ! Barista.PrepareCoffee(Coffee.Akkaccino, testActorSystem.deadLetters)
        sender.expectMsg(Barista.CoffeePrepared(Coffee.Akkaccino, testActorSystem.deadLetters))
      }
    }
  }
}
