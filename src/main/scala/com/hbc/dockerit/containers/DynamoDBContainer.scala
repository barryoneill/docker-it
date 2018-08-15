package com.hbc.dockerit.containers

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.hbc.dockerit.matchers.DynamoDBMatchers
import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait DynamoDBContainer extends DockerKit with ScalaFutures with DynamoDBMatchers {

  val AwsRegion = "us-east-1" // can be any region for dynamodb local

  private val AdvertisedPort = 8000

  private val Attempts = 25
  private val Delay    = 2500.milliseconds

  private lazy val dynamoDBHost: String = dockerExecutor.host
  private lazy val dynamoDBPort: Int    = getDynamoDBPort(container.getPorts().futureValue)

  lazy val dynamoDBURL: String          = s"http://$dynamoDBHost:$dynamoDBPort"
  lazy val dynamoDB: DynamoDB = buildClient(dynamoDBURL)

  private[this] val container: DockerContainer = DockerContainer("dwmkerr/dynamodb:38")
    .withPorts(AdvertisedPort -> None)
    .withReadyChecker(new DynamoDBReadyChecker().looped(Attempts, Delay))

  abstract override def dockerContainers: List[DockerContainer] = container :: super.dockerContainers

  private[this] def getDynamoDBPort(portsMapping: Map[Int, Int]) = {
    portsMapping.getOrElse(
      AdvertisedPort,
      Assertions.fail(s"Couldn't find mapping for port $AdvertisedPort. (Found: ${container.getPorts()})"))
  }

  def buildClient(url: String): DynamoDB = {
    val endpoint = new AwsClientBuilder.EndpointConfiguration(url, AwsRegion)
    lazy val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withEndpointConfiguration(endpoint).build
    new DynamoDB(client)
  }

  private[this] class DynamoDBReadyChecker extends DockerReadyChecker {

    override def apply(container: DockerContainerState)(implicit dockerExecutor: DockerCommandExecutor,
                                                        ec: ExecutionContext): Future[Boolean] = {

      Future {
        Try {
          val client = buildClient(dynamoDBURL)
          client.listTables(1)
          true
        }.getOrElse(false)
      }(ec)

    }
  }

}
