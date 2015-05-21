package com.boldradius.sdf.akka

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.boldradius.sdf.akka.ChatActor.StartChat
import com.boldradius.sdf.akka.StatsActor.StatsDump
import com.boldradius.sdf.akka.UserTrackerActor.Visit
import org.scalatest.{Matchers, WordSpec, FunSuite}
import akka.actor.ActorDSL._
import scala.concurrent.duration._
import org.joda.time.Duration

class UserTrackerActorSpec extends WordSpec with Matchers {

  implicit val system = ActorSystem()

  val mockActorProbe = TestProbe()

  "The tracker actor" should {
    "shutdown after 1s of inactivity" in {
      val probe = TestProbe()
      val shieldActor = actor(new ShieldActor(probe.ref, mockActorProbe.ref))
      probe.watch(shieldActor)
      probe.within(100 millis, 300 millis) {
        probe.expectMsg(StatsDump(List()))
        probe.expectTerminated(shieldActor)
      }
    }
    "store all requests and send them when session closed" in {
      val probe = TestProbe()
      val shieldActor = actor(new ShieldActor(probe.ref, mockActorProbe.ref))
      probe.watch(shieldActor)

      val r1 = Request(1, 1, "url1", "ref1", "b1")
      val r2 = Request(2, 2, "url2", "ref2", "b2")

      val v1 = Visit(r1, new Duration(1000))
      val v2 = Visit(r2, new Duration(2000))

      shieldActor ! r1
      Thread.sleep(100)
      shieldActor ! r2
      Thread.sleep(100)
      shieldActor ! r1

      probe.within(100 milliseconds, 300 milliseconds) {
        val dump = probe.expectMsgType[StatsDump]
        dump.requests.length shouldBe 3
        dump.requests(0).request shouldBe r1
        dump.requests(1).request shouldBe r2
        dump.requests(2).request shouldBe r1
        probe.expectTerminated(shieldActor)
      }
    }

    "send a chat request after timeout on help page" in {
      val probe = TestProbe()
      val shieldActor = actor(new ShieldActor(mockActorProbe.ref, probe.ref))

      val r1 = Request(1, 1, "/help", "ref1", "b1")
      val r2 = Request(1, 1, "/other", "ref1", "b1")

      shieldActor ! r1
      Thread.sleep(200)
      shieldActor ! r2

      shieldActor ! r1

      probe.expectMsg(StartChat(1))
    }

    "not send a chat request if page help left soon enough" in {
      val probe = TestProbe()
      val shieldActor = actor(new ShieldActor(mockActorProbe.ref, probe.ref))

      val r1 = Request(1, 1, "/help", "ref1", "b1")
      val r2 = Request(1, 1, "/other", "ref1", "b1")

      shieldActor ! r1
      Thread.sleep(50)
      shieldActor ! r2

      probe.expectNoMsg(500 millis)

    }

  }

}
