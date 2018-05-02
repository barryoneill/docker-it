package com.hbc.dockerit.containers

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.hbc.dockerit.matchers.{CloudWatchMatchers, KinesisMatchers}
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

trait LocalStackContainer extends DockerKit with ScalaFutures
                    with KinesisMatchers with CloudWatchMatchers {

  val PORT_WEB_UI = 8080
  val PORT_KINESIS = 4568
  val PORT_CLOUDWATCH = 4582
  val PORTS = Seq(PORT_WEB_UI, PORT_KINESIS, PORT_CLOUDWATCH)

  private[this] val container: DockerContainer = DockerContainer("localstack/localstack:0.8.6")
    .withPorts(PORTS.map(_ -> None): _*)
    .withReadyChecker(DockerReadyChecker.LogLineContains("Ready."))

  abstract override def dockerContainers: List[DockerContainer] = container :: super.dockerContainers

  // localstack uses kinesalite, which doesn't support CBOR (which the AWS SDK defaults to using)
  // https://github.com/mhart/kinesalite#cbor-protocol-issues-with-the-java-sdk
  System.setProperty("com.amazonaws.sdk.disableCbor", "1")

  private[this] def getMappedPort(port: Int): Int = {
    container.getPorts().futureValue
      .getOrElse(port, Assertions.fail(s"Couldn't find mapping for port $port. (Found: ${container.getPorts()})"))
  }

  lazy val portWebUI: Int = getMappedPort(PORT_WEB_UI)

  lazy val portCloudwatch: Int = getMappedPort(PORT_CLOUDWATCH)

  lazy val portKinesis: Int = getMappedPort(PORT_KINESIS)

  // a convenience dummy cred provider (localstack doesn't need auth, but the AWS SDK requires a provider)
  lazy val dummyAWSCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials("fart", "fart"))

}