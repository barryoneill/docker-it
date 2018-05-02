package com.hbc.dockerit.containers

import com.hbc.dockerit.matchers.RedisMatchers
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

trait RedisContainer extends DockerKit with ScalaFutures with RedisMatchers {

  val PORT = 6379

  private[this] val container: DockerContainer = DockerContainer("redis:3.2.11")
    .withPorts(PORT -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("The server is now ready to accept connections"))

  abstract override def dockerContainers: List[DockerContainer] = container :: super.dockerContainers

  lazy val redisPort: Int = container.getPorts().futureValue.getOrElse(PORT, Assertions.fail(s"Couldn't find mapping for port $PORT. (Found: ${container.getPorts()})"))

}