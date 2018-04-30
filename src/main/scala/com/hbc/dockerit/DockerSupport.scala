package com.hbc.dockerit

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.hbc.dockerit.util.EnvWriter
import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.{DockerContainer, DockerFactory, DockerReadyChecker}
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.Suite
import org.scalatest.time.{Second, Seconds, Span}

import scala.concurrent.duration._

trait DockerSupport extends DockerTestKit { self: Suite =>

  private[this] object Redis {
    val IMAGE = "redis:3.2.11"
    val READYTEXT = "The server is now ready to accept connections"
    val PORT = 6379
  }

  private[this] object LocalStack {
    val IMAGE = "localstack/localstack:0.8.6"
    val READYTEXT = "Ready."
    val PORT_WEB_UI = 8080
    val PORT_KINESIS = 4568
    val PORT_CLOUDWATCH = 4582
    val PORTS = Seq(PORT_WEB_UI, PORT_KINESIS, PORT_CLOUDWATCH)
  }

  /* Redis container is fast, but thanks to the likes of elasticsearch, the localstack container takes approx
     25 seconds on my macbook before all services are available and the 'Ready' log message appears */
  override val StartContainersTimeout: FiniteDuration = 60.seconds

  private[this] implicit val pc: PatienceConfig = PatienceConfig(Span(60, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  // Don't bind to the known ports (hence `-> None`), since an aborted test can leave a container hogging those ports
  // (also, a dev may be using them locally anyway, especially in the case of redis)
  // Callers should use get getMapped* methods below to lookup the ephemeral port that docker chose for the mapping.
  val redisContainer: DockerContainer = DockerContainer(Redis.IMAGE)
    .withPorts(Redis.PORT -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains(Redis.READYTEXT))

  val localstackContainer: DockerContainer = DockerContainer(LocalStack.IMAGE)
    .withPorts(LocalStack.PORTS.map(_ -> None): _*)
    .withEnv("DEBUG=1")
    .withReadyChecker(DockerReadyChecker.LogLineContains(LocalStack.READYTEXT))

  abstract override def dockerContainers: List[DockerContainer] =
    redisContainer :: localstackContainer :: super.dockerContainers

  override def beforeAll(): Unit = {

    println("Waiting for redis and localstack containers to be ready (takes a little while - even more so if the images have to be fetched as well)")

    super.beforeAll()

    if (!isContainerReady(redisContainer).futureValue) {
      fail("Redis container wasn't ready")
    }

    if (!isContainerReady(localstackContainer).futureValue) {
      fail("Localstack container wasn't ready")
    }

    println("Containers ready.  Mapped ports are: ")
    println(s" - redis (${redisContainer.getName().futureValue}): ${redisContainer.getPorts().futureValue}")
    println(s" - localstack (${localstackContainer.getName().futureValue}): ${localstackContainer.getPorts().futureValue}")
    println(s"\nWhile the containers are alive, visit http://localhost:$getMappedWebUIPort to view status of localstack resources")

    // localstack uses kinesalite, which doesn't support CBOR (which the AWS SDK defaults to using)
    // https://github.com/mhart/kinesalite#cbor-protocol-issues-with-the-java-sdk
    EnvWriter.setEnvVar("AWS_CBOR_DISABLE", "true")

  }

  override def afterAll(): Unit = {

    println("Shutting down containers.")
    super.afterAll()

  }

  // a convenience dummy cred provider (localstack doesn't need auth, but the AWS SDK requires a provider)
  val localstackAWSCredProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("fart", "fart"))

  private[this] def getMappedPort(port: Int): Int = {
    val ports = redisContainer.getPorts().futureValue ++ localstackContainer.getPorts().futureValue
    ports.getOrElse(port, fail(s"Couldn't find mapping for port $port. (Found: $ports)"))
  }

  def getMappedRedisPort: Int = getMappedPort(Redis.PORT)

  def getMappedWebUIPort: Int = getMappedPort(LocalStack.PORT_WEB_UI)

  def getMappedKinesisPort: Int = getMappedPort(LocalStack.PORT_KINESIS)

  def getMappedCloudWatchPort: Int = getMappedPort(LocalStack.PORT_CLOUDWATCH)

}
