package com.hbc.dockerit.util

import java.util.Date

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.{ Datapoint, GetMetricStatisticsRequest }

import scala.collection.JavaConverters._

/**
  * A wrapper for the kineis client, providing a bunch of convenience methods to save clutter in tests/matchers.
  */
case class CloudWatchUtil(client: AmazonCloudWatch) {

  def listMetrics(
    namespace: String,
    name: String,
    period: Int = 60,
    relativeSecs: Int = 60
  ): Seq[Datapoint] =
    client
      .getMetricStatistics(
        new GetMetricStatisticsRequest()
          .withNamespace(namespace)
          .withMetricName(name)
          .withPeriod(period)
          .withStartTime(new Date(new Date().getTime - (relativeSecs * 1000)))
          .withEndTime(new Date())
          .withStatistics("Sum", "Average", "Maximum")
      )
      .getDatapoints
      .asScala

}
