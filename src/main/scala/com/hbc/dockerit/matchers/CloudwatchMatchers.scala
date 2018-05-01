package com.hbc.dockerit.matchers

import java.util.Date

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.cloudwatch.model.{Datapoint, GetMetricStatisticsRequest}
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.hbc.dockerit.containers.LocalstackContainer
import org.scalatest.Matchers

import scala.collection.JavaConverters._

trait CloudwatchMatchers extends Matchers {
  container: LocalstackContainer =>

  lazy val cloudwatch: AmazonCloudWatch = AmazonCloudWatchClientBuilder.standard()
    .withCredentials(container.dummyAWSCreds)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$portCloudwatch/", "us-east-1"))
    .build()

  def cloudWatchListMetrics(namespace: String, name: String): Seq[Datapoint] = {

    cloudwatch.getMetricStatistics(new GetMetricStatisticsRequest()
      .withNamespace(namespace)
      .withMetricName(name)
      .withPeriod(60)
      .withStartTime(new Date(new Date().getTime - 60000))
      .withEndTime(new Date())
      .withStatistics("Sum", "Average", "Maximum")).getDatapoints.asScala

  }


}