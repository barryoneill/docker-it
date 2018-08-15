package com.hbc.dockerit

import java.util.UUID

import com.hbc.dockerit.containers.DynamoDBContainer
import com.hbc.dockerit.model.dynamodb._
import com.hbc.dockerit.util.DynamoDBUtil
import org.scalatest.WordSpec

class DynamoDBContainerSpec extends WordSpec with DockerSuite with DynamoDBContainer {

  lazy val dynamoDBUtil = DynamoDBUtil(dynamoDB)

  def genTableName(): String = s"TestTable-${UUID.randomUUID()}"

  "DynamoDBContainer" should {

    "start a dynamodb instance" that {

      "is available" in {
        dynamoDBUtil.ping should be(true)
      }
    }

    "have utils" that {

      "create a table" in {

        val TableName = genTableName()
        val createTableResult = dynamoDBUtil.createTable(
          tableName = TableName,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )
        createTableResult should be(Right(Some(TableDescription(name = TableName))))
        dynamoDBUtil.tableExists(TableName) should be(Right(true))
      }

      "do not fail when creating a table that already exists" in {

        val TableName = genTableName()
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
      }

      "delete a table" in {
        val TableName = genTableName()

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
        val TableName    = genTableName()
        val deleteResult = dynamoDBUtil.deleteTable(TableName)
        deleteResult should be(Right(None))
      }
    }

  }

  "have dynamodb matchers" that {

    "verify that a table exists" in {

      val TableName    = "TestTable"
      val dynamoDBUtil = DynamoDBUtil(dynamoDB)

      // ensure table is deleted
      dynamoDBUtil.deleteTable(TableName)
      dynamoDBUtil.createTable(tableName = TableName,
                               attributes = Seq(Key(name = "id", dataType = StringDataType, PartitionKeyType)))

      dynamoDB should haveTable("TestTable")
      dynamoDB should not(haveTable("FarTable"))

    }

  }

}
