package com.hbc.dockerit

import java.util.UUID

import com.hbc.dockerit.containers.DynamoDBContainer
import com.hbc.dockerit.model.dynamodb._
import com.hbc.dockerit.util.DynamoDBUtil
import org.scalatest.{Assertion, WordSpec}
import org.scalatest.exceptions.TestFailedException

class DynamoDBContainerSpec extends WordSpec with DockerSuite with DynamoDBContainer {

  lazy val dynamoDBUtil = DynamoDBUtil(dynamoDB)

  def withTableName(f: String => Assertion): Assertion = f(s"TestTable-${UUID.randomUUID()}")

  "DynamoDBContainer" should {

    "start a dynamodb instance" that {

      "is available" in {
        dynamoDBUtil.ping should be(true)
      }
    }

    "have utils" that {

      "create a table" in withTableName { table =>

        val createTableResult = dynamoDBUtil.createTable(
          tableName = table,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )
        createTableResult should be(Right(Some(TableDescription(name = table))))
        dynamoDBUtil.tableExists(table) should be(Right(true))
      }

      "do not fail when creating a table that already exists" in withTableName { table =>

        val createTableResult = dynamoDBUtil.createTable(
          tableName = table,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createTableResult should be(Right(Some(TableDescription(name = table))))
        dynamoDBUtil.tableExists(table) should be(Right(true))

        // create table again
        val createAlreadyExistingTableResult = dynamoDBUtil.createTable(
          tableName = table,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createAlreadyExistingTableResult should be(Right(None))
        dynamoDBUtil.tableExists(table) should be(Right(true))
      }

      "delete a table" in withTableName { tableName =>

        val createTableResult = dynamoDBUtil.createTable(
          tableName = tableName,
          attributes = Seq(
            Key(name = "id", dataType = StringDataType, PartitionKeyType)
          )
        )

        createTableResult should be(Right(Some(TableDescription(name = tableName))))
        dynamoDBUtil.tableExists(tableName) should be(Right(true))

        val deleteTableResult = dynamoDBUtil.deleteTable(tableName)
        deleteTableResult should be(Right(Some(TableDescription(name = tableName))))
        dynamoDBUtil.tableExists(tableName) should be(Right(false))
      }

      "do not fail when deleting a table that doesn't exist" in withTableName { tableName =>
        val deleteResult = dynamoDBUtil.deleteTable(tableName)
        deleteResult should be(Right(None))
      }
    }

  }

  "have dynamodb matchers" that {

    "verify that a table exists" in withTableName { tableName =>

      val NoExistName = "non-existent-table"

      dynamoDBUtil.createTable(tableName = tableName,
                               attributes = Seq(Key(name = "id", dataType = StringDataType, PartitionKeyType)))

      dynamoDB should haveTable(tableName)
      dynamoDB should not(haveTable(NoExistName))

      the[TestFailedException] thrownBy {
        dynamoDB should haveTable(NoExistName)
      } should have message s"""Expected table "$NoExistName" to exist but it does not"""

      the[TestFailedException] thrownBy {
        dynamoDB should not(haveTable(tableName))
      } should have message s"""Did not expect table "$tableName" to exist"""

    }

    "verify that a new table has empty count" in withTableName { tableName =>

      dynamoDBUtil.createTable(tableName = tableName,
                               attributes = Seq(Key(name = "id", dataType = StringDataType, PartitionKeyType)))

      dynamoDB should haveItemCount(tableName, 0)
      dynamoDB should not(haveItemCount(tableName, 1))

      the[TestFailedException] thrownBy {
        dynamoDB should haveItemCount(tableName, 333)
      } should have message s"""Table "$tableName" has unexpected itemCount 0 (expected 333)"""

      the[TestFailedException] thrownBy {
        dynamoDB should not(haveItemCount(tableName, 0))
      } should have message s"""Table "$tableName" not expected to have itemCount 0 but it did"""

    }

  }

}
