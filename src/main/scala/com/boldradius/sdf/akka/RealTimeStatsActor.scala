package com.boldradius.sdf.akka

import akka.actor.{Actor, ActorLogging}
import com.boldradius.sdf.akka.RealTimeStatsActor.{UserLoggedOut, UserTrack}

import scala.collection.mutable

class RealTimeStatsActor extends Actor with ActorLogging {

  val userTrack: mutable.HashMap[Long, UserStat] = new mutable.HashMap[Long, UserStat]()

  override def receive: Receive = {
    case ut: UserTrack =>
      userTrack.update(ut.sessionId, UserStat(ut.url, ut.browser))

    case ulo: UserLoggedOut =>
      userTrack.remove(ulo.sessionId)
  }
}

case class UserStat(url:String, browser: String)

object RealTimeStatsActor {

  case class UserTrack(sessionId: Long, url: String, browser: String)

  case class UserLoggedOut(sessionId: Long)

  case object TotalUsersRequest
  case class TotalUsersResponse(count:Int)

  case object TotalUsersPerUrlRequest
  case class TotalUsersPerUrlResponse(usersPerUrl: Map[String,String])

  case object TotalUsersPerBrowserRequest
  case class TotalUsersPerBrowserResponse(usersPerBrowser: Map[String,String])
}
