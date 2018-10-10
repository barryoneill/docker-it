package com.hbc.dockerit.containers

import java.net.ServerSocket
import java.util.Properties

import com.hbc.dockerit.matchers.KafkaMatchers
import com.whisk.docker.{ContainerLink, DockerContainer, DockerKit, DockerReadyChecker}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.util.{Failure, Success, Try}

trait KafkaContainerClient {
  def bootstrapServer: String
  def zkServer: String
  def adminClient: AdminClient
  def stringProducer: KafkaProducer[String, String]
  def stringConsumer(groupId: String): KafkaConsumer[String, String]
}

trait KafkaContainer extends DockerKit with ScalaFutures with KafkaMatchers {

  object KafkaContainerClient extends KafkaContainerClient {
    lazy val bootstrapServer = s"${dockerExecutor.host}:$bootstrapMappedPort"
    lazy val zkServer        = s"${dockerExecutor.host}:$zkMappedPort"

    // an alternate client to the one in use, but looks like it might go away in a future kafka version
    // lazy val kafkaZkClient = KafkaZkClient(zkServer, isSecure = false, 5000, 5000, 5, Time.SYSTEM)
    // lazy val adminClient   = new AdminZkClient(kafkaZkClient)
    lazy val adminClient = AdminClient.create(adminClientProps())

    lazy val stringProducer = new KafkaProducer[String, String](stringProducerProps())
    def stringConsumer(groupId: String) = {
      println(s"Attaching consumer groupID to $bootstrapServer")
      new KafkaConsumer[String, String](stringConsumerProps(groupId))
    }

  }

  private[this] val ZOOKEEPER_PORT = 2181

  // need to know at least the zookeeper hostname in advance
  val zookeeperContainerName = s"zookeeper-${System.currentTimeMillis}"
  val kafkaContainerName     = s"kafka-${System.currentTimeMillis}"

  lazy val zkContainer: DockerContainer = DockerContainer("zookeeper:3.5", name = Some(zookeeperContainerName))
    .withPorts(ZOOKEEPER_PORT -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("binding to port /0.0.0.0"))

  /** Things to note:
    *  - we give the zk container above a name in advance so we can link it to this container
    *    - this lets us sets KAFKA_ZOOKEEPER_CONNECT to containername:zk-standard-port
    *  - kafka however, we define the ephemeral port in advance (instead of letting docker choose one)
    *    - this lets us actually run the kafka broker on the same ephemeral port number (as opposed to mapping it)
    *    - avoids MANY headaches trying to resolve docker mappings in kafka's advertised listener metadata config
    * */
  val bootstrapMappedPort: Int = newEphemeralPort()
  lazy val kafkaContainer: DockerContainer =
    DockerContainer("wurstmeister/kafka:2.11-2.0.0", name = Some(kafkaContainerName))
      .withLinks(ContainerLink(zkContainer, zookeeperContainerName))
      .withPorts(bootstrapMappedPort -> Some(bootstrapMappedPort))
      .withEnv(
        s"KAFKA_ZOOKEEPER_CONNECT=$zookeeperContainerName:$ZOOKEEPER_PORT",
        s"KAFKA_PORT=$bootstrapMappedPort",
        s"KAFKA_ADVERTISED_HOST_NAME=${dockerExecutor.host}",
      )
      .withReadyChecker(DockerReadyChecker.LogLineContains("] started (kafka.server.KafkaServer)"))

  abstract override def dockerContainers: List[DockerContainer] =
    kafkaContainer :: zkContainer :: super.dockerContainers

  private[this] lazy val zkMappedPort: Int = zkContainer
    .getPorts()
    .futureValue
    .getOrElse(
      ZOOKEEPER_PORT,
      Assertions.fail(s"Couldn't find mapping for port $ZOOKEEPER_PORT. (Found: ${kafkaContainer.getPorts()})"))

  private[this] def newEphemeralPort(): Int =
    Try {
      new ServerSocket(0)
    } match {
      case Success(sock) =>
        val port = sock.getLocalPort
        sock.close()
        port
      case Failure(err) => throw err
    }

  private[this] def adminClientProps(): Properties = {
    val props = new Properties()
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerClient.bootstrapServer)
    props
  }

  private[this] def stringProducerProps(): Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerClient.bootstrapServer)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, s"$zookeeperContainerName-client")
    props
  }

  private[this] def stringConsumerProps(groupId: String, fromBeginning: Boolean = true): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerClient.bootstrapServer)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getCanonicalName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getCanonicalName)
    if(fromBeginning){
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props
  }



}
