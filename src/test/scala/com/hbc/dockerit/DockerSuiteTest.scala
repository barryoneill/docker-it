package com.hbc.dockerit

import java.sql.{DriverManager, Statement}

import com.hbc.dockerit.containers.{DynamoDBContainer, LocalStackContainer, PostgresContainer, RedisContainer}
import com.hbc.dockerit.model.dynamodb._
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.{DynamoDBUtil, KinesisUtil, RedisUtil}
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class DockerSuiteTest extends WordSpec with BeforeAndAfterAll
  with DockerSuite with RedisContainer with LocalStackContainer with PostgresContainer with DynamoDBContainer {

  def compare: AfterWord = afterWord("compare")

  object RedisTestData {
    val AnimalNoiseMap = Map("cat" -> "miaow", "dog" -> "woof", "duck" -> "quack", "human" -> "talk")
    val AccountKey = "savingsacc"
    val AccountVal = BankAccount("1234-RICH-GUY", 99999.99)

  }

  object KinesisTestData {
    val StreamName = "testStream"
    val BankAccounts = Seq(BankAccount("One", 1111), BankAccount("Two", 2222), BankAccount("Three", 333))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    val redisUtil = RedisUtil(redis)
    redisUtil.resetCache()
    redisUtil.mSet(RedisTestData.AnimalNoiseMap)
    redisUtil.set(RedisTestData.AccountKey, RedisTestData.AccountVal)

    val kinesisUtil = KinesisUtil(kinesis)
    kinesisUtil.createStream(KinesisTestData.StreamName)
    kinesisUtil.putRecords[BankAccount](KinesisTestData.StreamName, KinesisTestData.BankAccounts, r => r.accountNumber)
  }

  "RedisContainer" should {

    "have redis matchers" that compare {

      "haveOnlyKeys" in {
        val expectedKeys = RedisTestData.AccountKey :: RedisTestData.AnimalNoiseMap.keySet.toList
        redis should haveOnlyKeys(expectedKeys: _*)
      }

      "haveKeys" in {
        redis should haveKeys("human", "dog")
        redis should not(haveKeys("platypus"))
      }

      "haveKeysMatching" in {
        redis should haveKeysMatching("*o*")
        redis should not(haveKeysMatching("fart"))
      }

      "haveValue (string)" in {
        redis should haveValueOnGet("dog", "woof")
      }

      "haveValue (encoded)" in {
        redis should haveEncodedValueOnGet(RedisTestData.AccountKey, RedisTestData.AccountVal)
      }
    }

  }

  "LocalstackContainer" should {

    "have kinesis matchers" that compare {

      "havePendingEvents" in {
        kinesis should havePendingEvents(KinesisTestData.StreamName, KinesisTestData.BankAccounts)
      }

    }

    //    "have cloudwatch matchers" that compare {
    //
    //      "havePublishedMetrics" in {
    //
    //        // TODO: coming soon, eventually, at some point
    //        3 shouldBe 3
    //      }
    //
    //    }

  }


  "PostgresContainer" should {

    def newConnection = DriverManager.getConnection(
      Postgres.url, Postgres.user, Postgres.password)


    def dbStatement(f: Statement => Unit): Unit = {
      val conn = newConnection
      try {
        val stmt = conn.createStatement()
        f(stmt)
      } finally {
        if (conn != null)
          conn.close()
      }
    }

    "start a postgres instance" that {

      "is available" in {
        dbStatement(stmt => {
          val rs = stmt.executeQuery("select 1")
          rs.next() should be(true)
          rs.getInt(1) should be(1)
        })
      }
    }
  }


  "DynamoDBContainer" should {

    "start a dynamodb instance" that {

      "is available" in {
        val dynamoDBUtil = DynamoDBUtil(DynamoDB.client)
        dynamoDBUtil.ping should be (true)
      }

    }

    "have utils" that {

      "create a table" in {
        val TableName = "TestTable"
        val dynamoDBUtil = DynamoDBUtil(DynamoDB.client)

        // ensure table is deleted
        dynamoDBUtil.deleteTable(TableName)

        val createTableResult = dynamoDBUtil.createTable(
          tableName = TableName,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createTableResult should be(Right(Some(TableDescription(name = TableName))))
        dynamoDBUtil.tableExists(TableName) should be(Right(true))

        // clean created table
        dynamoDBUtil.deleteTable(TableName)
      }

      "do not fail when creating a table that already exists" in {
        val TableName = "TestTable"
        val dynamoDBUtil = DynamoDBUtil(DynamoDB.client)

        // ensure table is deleted
        dynamoDBUtil.deleteTable(TableName)

        val createTableResult = dynamoDBUtil.createTable(
          tableName = TableName,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createTableResult should be(Right(Some(TableDescription(name = TableName))))
        dynamoDBUtil.tableExists(TableName) should be(Right(true))

        // create table again
        val createAlreadyExistingTableResult = dynamoDBUtil.createTable(
          tableName = TableName,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createAlreadyExistingTableResult should be(Right(None))
        dynamoDBUtil.tableExists(TableName) should be(Right(true))

        // clean created table
        dynamoDBUtil.deleteTable(TableName)
      }

      "delete a table" in {
        val TableName = "TestTable"
        val dynamoDBUtil = DynamoDBUtil(DynamoDB.client)

        // ensure table is deleted
        dynamoDBUtil.deleteTable(TableName)

        val createTableResult = dynamoDBUtil.createTable(
          tableName = TableName,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createTableResult should be(Right(Some(TableDescription(name = TableName))))
        dynamoDBUtil.tableExists(TableName) should be(Right(true))

        val deleteTableResult = dynamoDBUtil.deleteTable(TableName)
        deleteTableResult should be(Right(Some(TableDescription(name = TableName))))
        dynamoDBUtil.tableExists(TableName) should be(Right(false))
      }

      "do not fail when deleting a table that doesn't exist" in {
        val TableName = "TestTable"
        val dynamoDBUtil = DynamoDBUtil(DynamoDB.client)

        val deleteResult = dynamoDBUtil.deleteTable(TableName)
        deleteResult should be(Right(None))
      }
    }

  }

}
