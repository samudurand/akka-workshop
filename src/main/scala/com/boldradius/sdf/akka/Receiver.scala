package com.boldradius.sdf.akka

import akka.actor._
import com.boldradius.sdf.akka.RealTimeStatsActor.{UserLoggedOut, UserTrack}
import com.boldradius.sdf.akka.ShieldActor.BannedRequest

import scala.collection.mutable

class Receiver(statsActor: ActorRef, emailActor: ActorRef) extends Actor with ActorLogging {
  private val actorsBySession = mutable.HashMap.empty[Long, ActorRef]
  private val realTimeStatsActor = context.actorOf(RealTimeStatsActor.props)

  def receive: Receive = {
    case request: Request =>
      val actor = getFromSessionOrCreate(request.sessionId)
      actor ! request
      realTimeStatsActor ! UserTrack(request.sessionId, request.url, request.browser)
    case BannedRequest(request) => log.info("Reject request from session {}", request.sessionId)
    case Terminated(tracker) =>
      val sessionId = actorsBySession.find(_._2 == tracker).get._1
      actorsBySession -= sessionId
      realTimeStatsActor ! UserLoggedOut(sessionId)

  }

  def getFromSessionOrCreate(sessionId: Long): ActorRef = {
    actorsBySession.get(sessionId) match {
      case Some(actor) =>
        actor
      case None =>
        createTracker(sessionId)
    }
  }

  private[akka] def createTracker(sessionId: Long): ActorRef = {
    val newTracker = context.actorOf(ShieldActor.props(statsActor, emailActor))
    actorsBySession += (sessionId -> newTracker)
    context.watch(newTracker)
    newTracker
  }
}

object Receiver {
  def props(statsActor: ActorRef, emailActor: ActorRef): Props = Props(new Receiver(statsActor, emailActor))
}

