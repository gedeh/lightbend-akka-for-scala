/**
 * Copyright © 2014 - 2020 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */

package com.lightbend.training.coffeehouse

import akka.testkit.{ EventFilter, TestProbe }

class CoffeeHouseSpec extends BaseAkkaSpec {

  "Creating CoffeeHouse" should {
    "result in logging a status message at info" in {
      EventFilter.info(pattern = ".*[Oo]pen.*", occurrences = 1) intercept {
        system.actorOf(CoffeeHouse.props)
      }
    }
  }

  "Sending a message to CoffeeHouse" should {
    "result in logging 'Coffee Brewing' at info" in {
      val sender = TestProbe()
      val coffeeHouse = system.actorOf(CoffeeHouse.props)
      EventFilter.info(source = coffeeHouse.path.toString, pattern = ".*[Cc]offee.*", occurrences = 1) intercept {
        coffeeHouse ! "Brew Coffee"
      }
    }
    "result in sending a 'Coffee Brewing' message as response" in {
      val sender = TestProbe()
      val coffeeHouse = system.actorOf(CoffeeHouse.props)
      sender.send(coffeeHouse, "Brew Coffee")
      sender.expectMsgPF() { case message if message.toString matches ".*[Cc]offee.*" => () }
    }
  }
}
