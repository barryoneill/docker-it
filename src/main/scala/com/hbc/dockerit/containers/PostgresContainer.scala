package com.hbc.dockerit.containers

import java.sql.{Connection, DriverManager, ResultSet}

import com.whisk.docker.{DockerCommandExecutor, DockerContainer, DockerContainerState, DockerKit, DockerReadyChecker}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

trait PostgresContainer extends DockerKit with ScalaFutures {

  object Postgres {
    val advertisedPort = 5432

    val user = "test"
    val password = "T3sT"

    lazy val host: String = dockerExecutor.host
    lazy val port: Int = getPostgresPort(container.getPorts().futureValue)
    lazy val url: String = buildUrl(host, port)
  }

  private[this] val container: DockerContainer = DockerContainer("postgres:9.6.3")
    .withPorts(Postgres.advertisedPort -> None)
    .withEnv(s"POSTGRES_USER=${Postgres.user}", s"POSTGRES_PASSWORD=${Postgres.password}")
    .withReadyChecker(new PostgresReadyChecker().looped(60, 500.milliseconds))

  abstract override def dockerContainers: List[DockerContainer] = container :: super.dockerContainers

  private[containers] def getPostgresPort(portsMapping: Map[Int, Int]) = {
    portsMapping.getOrElse(Postgres.advertisedPort, Assertions.fail(s"Couldn't find mapping for port ${Postgres.advertisedPort}. (Found: ${container.getPorts()})"))
  }

  private [containers] def buildUrl(host: String, port: Int): String = s"jdbc:postgresql://${host}:${port}/"

  private[containers] class PostgresReadyChecker extends DockerReadyChecker {

    override def apply(container: DockerContainerState)(implicit dockerExecutor: DockerCommandExecutor,
                                                        ec: ExecutionContext) = {

      def getConnection(host: String, port: Int): Connection = DriverManager.getConnection(
        buildUrl(host, port),
        Postgres.user,
        Postgres.password)

      container.getPorts()(dockerExecutor, ec).map(portsMapping => Try {
        val host = dockerExecutor.host
        val port = getPostgresPort(portsMapping)

        val rs: ResultSet = getConnection(host, port)
          .createStatement()
          .executeQuery("select 1")

        rs.next() && rs.getInt(1) == 1
      }.getOrElse(false))(ec)
    }
  }

}
