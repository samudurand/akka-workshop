package com.boldradius.sdf.akka

import akka.actor.{Actor, ActorLogging, Props}
import com.boldradius.sdf.akka.StatsActor.StatsDump
import com.boldradius.sdf.akka.UserTrackerActor.Visit
import org.joda.time.DateTime

import scala.collection.mutable

class StatsActor extends Actor with ActorLogging {

  /**
   * Total for browser, keyed by browser
   */
  val requestsPerBrowser = new mutable.HashMap[String, Int]() withDefaultValue 0

  /**
   * Totals for minute, keyed by minute of the day
   */
  val hitsPerMinute = new mutable.HashMap[Int, Int]() withDefaultValue 0

  /**
   * Total page visits keyed by page
   */
  val pageVisitDistribution = new mutable.HashMap[String, Int]() withDefaultValue 0

  val sinkPageVisitDistribution = new mutable.HashMap[String, Int]() withDefaultValue 0

  /**
   * Total referrers, keyed by referrer
   */
  val referrerDistribution = new mutable.HashMap[String, Int]() withDefaultValue 0

  /**
   * Count and averageTime per Url
   */
  val visitTimeDistribution = new mutable.HashMap[String, Visit]() withDefaultValue Visit(0, 0)

  case class Visit(count: Int, avTime: Long)

  override def receive: Receive = {
    case StatsDump(visits) =>
      log.info(s"Received ${visits.size} requests - updating stats")
      visits.foreach(visit => {
        calculateRequestsPerBrowser(visit.request.browser)
        calculateHitsPerMinute(visit.request.timestamp)
        calculatePageVisitDistribution(visit.request.url)
        calculateReferrerDistribution(visit.request.referrer)
        calculateVisitTimeAverage(visit.request.url, visit.duration)
      })

      calculateSinkPageVisitDistribution(visits.reverse.head.request.url)

    case _ => log.info("Stat received!")
  }

  //(Minute->count)
  def getBusiestMinuteOfDay: (Int, Int) = {
    def sorted = hitsPerMinute.toSeq.sortWith(_._2 > _._2)
    sorted.head
  }

  def getPageVisitDistribution(): Map[String, Double] = {
    val total = totalVisits
    pageVisitDistribution.map(pageCount => {
      (pageCount._1, pageCount._2.toDouble / total)
    }).toMap
  }

  private def totalVisits: Int = {
    pageVisitDistribution.foldLeft(0)(_ + _._2)
  }

  private def averageVisitTime: Map[String, Long] = {
    visitTimeDistribution.map(pair => (pair._1, pair._2.avTime)).toMap
  }

  def top3LandingPages(): Seq[String] = {
    val sorted = pageVisitDistribution.toSeq.sortWith((url, url2) => url._2 > url2._2)
    sorted.take(3).map(_._1)
  }

  def top3SinkPages() = {
    val sorted = sinkPageVisitDistribution.toSeq.sortWith((url, url2) => url._2 > url2._2)
    sorted.take(3).map(_._1)
  }

  def top2Browsers() = {
    val sorted = requestsPerBrowser.toSeq.sortWith(_._2 > _._2)
    sorted.take(3).map(_._1)
  }

  def top2Referrers() = {
    val sorted = referrerDistribution.toSeq.sortWith(_._2 > _._2)
    sorted.take(2).map(_._1)
  }

  private[akka] def calculateRequestsPerBrowser(browser: String) = {
    val requestPerBrowser = requestsPerBrowser(browser) + 1
    requestsPerBrowser.update(browser, requestPerBrowser)
    log.info(s"Updated requestsPerBrowser, for [${browser}] to: [${requestPerBrowser}]")
  }

  private[akka] def calculateHitsPerMinute(timestamp: Long) = {
    val minuteOfDay = new DateTime(timestamp).getMinuteOfDay
    val count = hitsPerMinute(minuteOfDay) + 1
    hitsPerMinute.update(minuteOfDay, count)
    log.info(s"Updated busiestMinuteOfTheDate, for min: [${minuteOfDay}] to count: [${count}]")
  }

  private[akka] def calculatePageVisitDistribution(url: String) = {
    val newCount = pageVisitDistribution(url) + 1
    pageVisitDistribution.update(url, newCount)
    log.info(s"Updated page count for url:[${url}] to [${newCount}]")
  }

  private[akka] def calculateSinkPageVisitDistribution(url: String) = {
    val newCount = sinkPageVisitDistribution(url) + 1
    sinkPageVisitDistribution.update(url, newCount)
    log.info(s"Updated sink page count for url:[${url}] to [${newCount}]")
  }

  private[akka] def calculateReferrerDistribution(referrer: String) = {
    val newCount = referrerDistribution(referrer) + 1
    referrerDistribution.update(referrer, newCount)
    log.info(s"Updated referrer count for referrer:[${referrer}] to [${newCount}]")
  }

  private[akka] def calculateVisitTimeAverage(url: String, duration: Long) = {
    val visit = visitTimeDistribution(url)
    val newCount = visit.count + 1
    val newAverage = (visit.count * visit.avTime + duration) / (visit.count + 1)

    visitTimeDistribution.update(url, Visit(newCount, newAverage))
    log.info(s"Updated visit time average for url:[${url}] to [${newAverage}]")
  }
}

object StatsActor {
  def props: Props = Props(new StatsActor)

  case class StatsDump(requests: List[Visit])

}