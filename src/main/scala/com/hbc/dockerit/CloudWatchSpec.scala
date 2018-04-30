package com.hbc.dockerit

import java.util.Date

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.cloudwatch.model.{Datapoint, GetMetricStatisticsRequest}
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}

import scala.collection.JavaConverters._

trait CloudWatchSpec {
  dockerSupport: DockerSupport =>

  def buildCloudWatchClient: AmazonCloudWatch = AmazonCloudWatchClientBuilder.standard()
    .withCredentials(dockerSupport.localstackAWSCredProvider)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$getMappedCloudWatchPort/", "us-east-1"))
    .build()

  def cloudWatchListMetrics(namespace: String, name: String): Seq[Datapoint] = {

    buildCloudWatchClient.getMetricStatistics(new GetMetricStatisticsRequest()
      .withNamespace(namespace)
      .withMetricName(name)
      .withPeriod(60)
      .withStartTime(new Date(new Date().getTime - 60000))
      .withEndTime(new Date())
      .withStatistics("Sum", "Average", "Maximum")).getDatapoints.asScala

  }


}
