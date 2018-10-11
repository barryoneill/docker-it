package com.hbc.dockerit.containers

import java.sql.{Connection, DriverManager, ResultSet}

import com.hbc.dockerit.matchers.PostgresMatchers
import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

trait PostgresContainer extends DockerKit with ScalaFutures with PostgresMatchers {

  private val AdvertisedPort = 5432

  private val TimeoutSeconds = 25
  private val Attempts       = 25
  private val Delay          = 2500.milliseconds

  object Postgres {
    val user     = "test"
    val password = "T3sT"

    lazy val host: String = dockerExecutor.host
    lazy val port: Int    = getPostgresPort(container.getPorts().futureValue)
    lazy val url: String  = buildUrl(host, port)
  }

  private[this] val container: DockerContainer = DockerContainer("postgres:10.4")
    .withPorts(AdvertisedPort -> None)
    .withEnv(s"POSTGRES_USER=${Postgres.user}", s"POSTGRES_PASSWORD=${Postgres.password}")
    .withReadyChecker(new PostgresReadyChecker().looped(Attempts, Delay))

  abstract override def dockerContainers: List[DockerContainer] = container :: super.dockerContainers

  private[this] def getPostgresPort(portsMapping: Map[Int, Int]) = {
    portsMapping.getOrElse(
      AdvertisedPort,
      Assertions.fail(s"Couldn't find mapping for port $AdvertisedPort. (Found: ${container.getPorts()})"))
  }

  private[this] def buildUrl(host: String, port: Int): String = s"jdbc:postgresql://$host:$port/?loggerLevel=OFF"

  private[this] class PostgresReadyChecker extends DockerReadyChecker {

    override def apply(container: DockerContainerState)(implicit dockerExecutor: DockerCommandExecutor,
                                                        ec: ExecutionContext): Future[Boolean] = {

      def getConnection(host: String, port: Int): Connection =
        DriverManager.getConnection(buildUrl(host, port), Postgres.user, Postgres.password)

      container
        .getPorts()(dockerExecutor, ec)
        .map(portsMapping =>
          Try {
            val host = dockerExecutor.host
            val port = getPostgresPort(portsMapping)

            getConnection(host, port)
              .isValid(TimeoutSeconds)
          }.getOrElse(false))(ec)
    }
  }

}
