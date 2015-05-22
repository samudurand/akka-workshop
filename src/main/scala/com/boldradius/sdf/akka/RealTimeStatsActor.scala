package com.boldradius.sdf.akka

import akka.actor.{Props, Actor, ActorLogging}
import com.boldradius.sdf.akka.RealTimeStatsActor._
import scala.concurrent.duration._
import scala.collection.mutable

class RealTimeStatsActor extends Actor with ActorLogging {

  import context.dispatcher

  val userTrack: mutable.HashMap[Long, UserStat] = new mutable.HashMap[Long, UserStat]()

  val s = context.system.scheduler.schedule(5 seconds, 10 seconds, self, TotalUsersRequest)
  val s2 = context.system.scheduler.schedule(10 seconds, 10 seconds, self, TotalUsersPerUrlRequest)

  override def receive: Receive = {
    case ut: UserTrack =>
      userTrack.update(ut.sessionId, UserStat(ut.url, ut.browser))
    case ulo: UserLoggedOut =>
      userTrack.remove(ulo.sessionId)
    case TotalUsersRequest =>
      TotalUsersResponse(userTrack.size)
    case TotalUsersPerUrlRequest =>
      TotalUsersPerUrlResponse(getUsersPerURL)
    case TotalUsersPerBrowserRequest =>
      TotalUsersPerBrowserResponse(getUsersPerBrowser)
  }

  private[akka] def getUsersPerURL(): Map[String, Int] = {
    userTrack.map(_._2.url).groupBy(_).mapValues(_.size)
  }

  private[akka] def getUsersPerBrowser(): Map[String, Int] = {
    userTrack.map(_._2.browser).groupBy(_).mapValues(_.size)
  }
}

case class UserStat(url: String, browser: String)

object RealTimeStatsActor {

  def props: Props = Props(new RealTimeStatsActor)

  case class UserTrack(sessionId: Long, url: String, browser: String)

  case class UserLoggedOut(sessionId: Long)

  case object TotalUsersRequest

  case class TotalUsersResponse(count: Int)

  case object TotalUsersPerUrlRequest

  case class TotalUsersPerUrlResponse(usersPerUrl: Map[String, Int])

  case object TotalUsersPerBrowserRequest

  case class TotalUsersPerBrowserResponse(usersPerBrowser: Map[String, Int])

}
