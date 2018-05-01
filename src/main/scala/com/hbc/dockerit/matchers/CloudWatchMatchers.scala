package com.hbc.dockerit.matchers

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.hbc.dockerit.containers.LocalStackContainer
import org.scalatest.Matchers

trait CloudWatchMatchers extends Matchers {
  container: LocalStackContainer =>

  lazy val cloudwatch: AmazonCloudWatch = AmazonCloudWatchClientBuilder.standard()
    .withCredentials(container.dummyAWSCreds)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$portCloudwatch/", "us-east-1"))
    .build()

  // TODO: actually implement some matchers
}