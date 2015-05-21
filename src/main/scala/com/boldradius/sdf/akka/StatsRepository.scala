package com.boldradius.sdf.akka

import java.io.{PrintWriter, File}

import akka.event.LoggingAdapter
import com.typesafe.config.Config
import play.api.libs.json.Json

import scala.io.Source

class StatsRepository(log: LoggingAdapter, config: Config) {

  implicit val visitAvFormat = Json.format[VisitAv]
  implicit val statsFormat = Json.format[StatsModel]

  val statsFile = config.getString("stats.file")

  def saveStatistics(statistics: Statistics): Unit = {
    log.info("Saving Json to file")

    val statsModel = statisticsToStatsModel(statistics)

    val json = Json.toJson(statsModel)
    val pw = new PrintWriter(new File(statsFile))

    pw.write(Json.prettyPrint(json))
    pw.close
  }

  def loadStatistics(): Statistics = {
    log.info("Loading stats from file")
    val string = Source.fromFile(new File(statsFile)).mkString

    val stats = Json.parse(string).as[StatsModel]
    statsModelToStatistics(stats)
  }

  /**
   * Convert mutable statistics model to immutable serializable version.
   */
  private[akka] def statisticsToStatsModel(statistics: Statistics): StatsModel = {
    StatsModel(
      statistics.requestsPerBrowser.toMap,
      statistics.hitsPerMinute.map(tuple => (tuple._1.toString, tuple._2)).toMap,
      statistics.pageVisitDistribution.toMap,
      statistics.sinkPageVisitDistribution.toMap,
      statistics.referrerDistribution.toMap,
      statistics.visitTimeDistribution.toMap)
  }

  /**
   * Convert immutable statsModel to mutable version
   */
  private[akka] def statsModelToStatistics(statsModel: StatsModel): Statistics = {
    new Statistics(
      collection.mutable.Map(statsModel.requestsPerBrowser.toSeq: _*),
      collection.mutable.Map(statsModel.hitsPerMinute.map(tuple => (tuple._1.toInt, tuple._2)).toSeq: _*),
      collection.mutable.Map(statsModel.pageVisitDistribution.toSeq: _*),
      collection.mutable.Map(statsModel.sinkPageVisitDistribution.toSeq: _*),
      collection.mutable.Map(statsModel.referrerDistribution.toSeq: _*),
      collection.mutable.Map(statsModel.visitTimeDistribution.toSeq: _*)
    )
  }

}

/**
 * Created by user on 21/05/15.
 */
case class StatsModel(
                       requestsPerBrowser: Map[String, Int],
                       hitsPerMinute: Map[String, Int], //int key
                       pageVisitDistribution: Map[String, Int],
                       sinkPageVisitDistribution: Map[String, Int],
                       referrerDistribution: Map[String, Int],
                       visitTimeDistribution: Map[String, VisitAv])
