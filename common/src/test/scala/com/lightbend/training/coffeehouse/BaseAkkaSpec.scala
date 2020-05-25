/**
 * Copyright © 2014 - 2020 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */

package com.lightbend.training.coffeehouse

import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Identify}
import akka.testkit.{EventFilter, TestEvent, TestProbe}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

abstract class BaseAkkaSpec extends BaseSpec with BeforeAndAfterAll {

  implicit class TestProbeOps(probe: TestProbe) {
    def expectActor(path: String, duration: FiniteDuration = 3.seconds): ActorRef = {
      probe.within(duration) {
        probe.awaitAssert {
          probe.system.actorSelection(path).tell(Identify(path), probe.ref)
          probe.expectMsgPF(duration) {
            case ActorIdentity(`path`, Some(ref)) => ref
            case ActorIdentity(`path`, None) => throw new RuntimeException(s"Unable to find actor with path $path in system $system after $duration")
          }
        }
      }
    }
  }

  implicit val system = ActorSystem("test-system")
  system.eventStream.publish(TestEvent.Mute(EventFilter.debug()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.info()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.warning()))
  system.eventStream.publish(TestEvent.Mute(EventFilter.error()))

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 20.seconds)
  }
}
