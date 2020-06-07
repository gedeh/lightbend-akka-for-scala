/**
 * Copyright © 2014 - 2020 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */

package com.lightbend.training.coffeehouse

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

import com.lightbend.training.coffeehouse.CoffeeHouse.GetStatus


import java.util.concurrent.TimeUnit
import scala.util.Success
import scala.util.Failure
import com.lightbend.training.coffeehouse.CoffeeHouse.Status

object CoffeeHouseApp {

  private val opt = """(\S+)=(\S+)""".r

  def main(args: Array[String]): Unit = {
    val opts = argsToOpts(args.toList)
    applySystemProperties(opts)
    val name = opts.getOrElse("name", "coffee-house")

    val system = ActorSystem(s"$name-system")
    val timeout = system.settings.config.getDuration("coffee-house.status-timeout", TimeUnit.MILLISECONDS).millis
    val coffeeHouseApp = new CoffeeHouseApp(system)(timeout)
    coffeeHouseApp.run()
  }

  private[coffeehouse] def argsToOpts(args: Seq[String]): Map[String, String] =
    args.collect { case opt(key, value) => key -> value }.to(Map)

  private[coffeehouse] def applySystemProperties(opts: Map[String, String]): Unit =
    for ((key, value) <- opts if key startsWith "-D")
      System.setProperty(key substring 2, value)
}

class CoffeeHouseApp(system: ActorSystem)(implicit statusTimeout: Timeout) extends Terminal {
  import system.dispatcher
  private val log = Logging(system, getClass.getName)
  private val caffeineLimit: Int = system.settings.config.getInt("coffee-house.caffeine-limit")
  private val coffeeHouse = createCoffeeHouse()

  def run(): Unit = {
    log.info("{} running. Enter commands into the terminal: [e.g. `q` or `quit`]", getClass.getSimpleName)
    commandLoop()
    Await.ready(system.whenTerminated, Duration.Inf)
  }

  protected def createCoffeeHouse(): ActorRef = {
    system.actorOf(CoffeeHouse.props(caffeineLimit), "coffee-house")
  }

  @tailrec
  private def commandLoop(): Unit = Command(StdIn.readLine()) match {
    case Command.Guest(count, coffee, caffeineLimit) =>
      createGuest(count, coffee, caffeineLimit)
      commandLoop()
    case Command.Status =>
      status()
      commandLoop()
    case Command.Quit =>
      system.terminate()
    case Command.Unknown(command) =>
      log.warning("Unknown command {}!", command)
      commandLoop()
  }

  protected def createGuest(count: Int, favoriteCoffee: Coffee, caffeineLimit: Int): Unit = {
    (1 to count).foreach {
      _ => coffeeHouse ! CoffeeHouse.CreateGuest(favoriteCoffee, caffeineLimit)
    }
  }

  protected def status(): Unit = {
    val result = coffeeHouse ? CoffeeHouse.GetStatus
    result.mapTo[CoffeeHouse.Status].onComplete {
      case Success(Status(guestCount)) => log.info(s"Status: guest count $guestCount")
      case Failure(exception) => log.error("Cant get status", exception)
    }
  }
}
