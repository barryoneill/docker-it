package com.hbc.dockerit

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.Suite

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

trait DockerSuite extends DockerTestKit { this: Suite =>

  /* Redis container is fast, but thanks to the likes of elasticsearch, the localstack container takes approx
     25 seconds on my macbook before all services are available and the 'Ready' log message appears */
  override val StartContainersTimeout: FiniteDuration = 60.seconds

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  override def beforeAll(): Unit = {

    if(dockerContainers.isEmpty) {
      fail(s"To use ${getClass.getSimpleName}, you must implement at least one container trait")
    }

    println("*** Waiting for containers ** (may take a little while - even more so if the images have to be fetched as well)")

    super.beforeAll()

    println("Containers ready.  Mapped ports are: ")

    dockerContainers.foreach(c => {
      println(s" - container (${c.getName().futureValue}): ${c.getPorts().futureValue}")
    })

  }

  override def afterAll(): Unit = {
    println("Shutting down containers.")
    super.afterAll()

  }


}
