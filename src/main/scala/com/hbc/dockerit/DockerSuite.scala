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


    log(s"            ----- containers starting ------    ")
    dockerContainers.foreach {
      dc => log(s" container '${dc.name.get}' starting with image '${dc.image}'..")
    }

    log(s" warning: may be slow, esp. if images have to be downloaded..")

    super.beforeAll()

    dockerContainers.foreach(c => {
      log(s"  - '${c.getName().futureValue}' ports: ")
      c.getPorts().futureValue.foreach(mapping => {
        log(s"       ${mapping._2} (local) => ${mapping._1} (container)" )
      })
    })

    log(s"            ----- containers started ------    ")

  }

  private[this] def log(msg: String){
    println(s"  -- hbc-docker-it   $msg") // TODO: add logging framework
  }

  override def afterAll(): Unit = {
    println("Shutting down containers.")
    super.afterAll()

  }


}
