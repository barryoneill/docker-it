package com.hbc.dockerit.containers

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

trait DynamoDBContainer extends DockerKit with ScalaFutures {

  val AwsRegion = "us-east-1" // can be any region for dynamodb local

  private val AdvertisedPort = 8000

  private val TimeoutSeconds = 25
  private val Attempts = 25
  private val Delay = 2500.milliseconds

  object DynamoDB {
    lazy val host: String = dockerExecutor.host
    lazy val port: Int = getDynamoDBPort(container.getPorts().futureValue)
    lazy val url: String = buildUrl(host, port)
    lazy val client: DynamoDB = buildClient(url)
  }

  private[this] val container: DockerContainer = DockerContainer("dwmkerr/dynamodb:latest")
    .withPorts(AdvertisedPort -> None)
    .withReadyChecker(new DynamoDBReadyChecker().looped(Attempts, Delay))

  abstract override def dockerContainers: List[DockerContainer] = container :: super.dockerContainers

  private[this] def getDynamoDBPort(portsMapping: Map[Int, Int]) = {
    portsMapping.getOrElse(AdvertisedPort, Assertions.fail(s"Couldn't find mapping for port $AdvertisedPort. (Found: ${container.getPorts()})"))
  }

  private[this] def buildUrl(host: String, port: Int): String = s"http://$host:$port"

  private[this] def buildClient(url: String): DynamoDB = {
    val endpoint = new AwsClientBuilder.EndpointConfiguration(
      url,
      AwsRegion)
    lazy val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard
      .withEndpointConfiguration(endpoint)
      .build
    new DynamoDB(client)
  }

  private[this] class DynamoDBReadyChecker extends DockerReadyChecker {

    override def apply(container: DockerContainerState)(implicit dockerExecutor: DockerCommandExecutor,
                                                        ec: ExecutionContext): Future[Boolean] = {

      container.getPorts()(dockerExecutor, ec).map(portsMapping => Try {
        val host = dockerExecutor.host
        val port = getDynamoDBPort(portsMapping)
        val url = buildUrl(host, port)

        val client = buildClient(url)
        client.listTables(1)
        true
      }.getOrElse(false))(ec)
    }
  }

}
