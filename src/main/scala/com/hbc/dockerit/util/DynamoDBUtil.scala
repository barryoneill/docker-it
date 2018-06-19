package com.hbc.dockerit.util

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.hbc.dockerit.errors.{Error, DynamoDBError}
import com.hbc.dockerit.model.dynamodb._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

case class DynamoDBUtil(client: DynamoDB) {

  val DefaultProvisionedThroughput = new Provisioning.Throughput(maxReads = 100L, maxWrites = 100L)

  def ping: Boolean =
    Try {
      tableExists("foo-bar")
    } match {
      case Success(_) => true
      case Failure(_) => false
    }

  def tableExists(tableName: String): Either[Error, Boolean] = {
    Try {
      val table = client.getTable(tableName)
      table.describe()
    } match {
      case Success(_) => Right(true)
      case Failure(e) =>
        if (e.isInstanceOf[ResourceNotFoundException]) Right(false)
        else Left(DynamoDBError(cause = Some(e)))
    }
  }

  def createTable(
      tableName: String,
      attributes: Seq[Attribute],
      throughput: Provisioning.Throughput = DefaultProvisionedThroughput): Either[Error, Option[TableDescription]] = {

    def validateAttributes: Option[Error] = {
      def countKeys(attributes: Seq[Attribute], K: KeyType): Int = attributes.count {
        case key: Key => key.keyType == K
        case _        => false
      }

      if (countKeys(attributes, PartitionKeyType) != 1)
        Some(DynamoDBError(msg = s"table <$tableName> must contain exactly 1 partition key"))
      else
        None
    }

    tableExists(tableName).flatMap {
      case true => Right(None)
      case false =>
        validateAttributes

        Try {
          val table = client.createTable(
            tableName,
            attributes.flatMap(a => Attribute.toAwsKey(a)).asJava,
            attributes.map(Attribute.toAwsAttribute).asJava,
            Provisioning.toAws(throughput)
          )

          table.waitForActive()
        } match {
          case Success(t) => Right(Some(TableDescription.fromAws(t)))
          case Failure(e) => Left(DynamoDBError(cause = Some(e)))
        }
      }
  }

  def deleteTable(tableName: String): Either[Error, Option[TableDescription]] = {
    Try {
      val table        = client.getTable(tableName)
      val deleteResult = table.delete()

      table.waitForDelete()
      deleteResult.getTableDescription
    } match {
      case Success(t) => Right(Some(TableDescription.fromAws(t)))
      case Failure(e) =>
        if (e.isInstanceOf[ResourceNotFoundException]) Right(None)
        else Left(DynamoDBError(cause = Some(e)))
    }
  }

}
