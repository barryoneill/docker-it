package com.hbc.dockerit

import com.amazonaws.services.dynamodbv2.model.{ AttributeDefinition => AwsAttribute }
import com.amazonaws.services.dynamodbv2.model.{ ScalarAttributeType => AwsAttributeType }
import com.amazonaws.services.dynamodbv2.model.{ KeySchemaElement => AwsKey }
import com.amazonaws.services.dynamodbv2.model.{ KeyType => AwsKeyType }
import com.amazonaws.services.dynamodbv2.model.{ ProvisionedThroughput => AwsProvisionedThroughput }
import com.amazonaws.services.dynamodbv2.model.{ TableDescription => AwsTableDescription }

object model {

  object dynamodb {

    sealed trait Attribute {
      def name: String
      def dataType: DataType
    }

    case class Key(name: String, dataType: DataType, keyType: KeyType) extends Attribute

    // TODO add secondary indexes (see com.amazonaws.services.dynamodbv2.model.CreateTableRequest.{localSecondaryIndexes, globalSecondaryIndexes}

    object Attribute {
      def toAwsAttribute(attribute: Attribute): AwsAttribute =
        new AwsAttribute(attribute.name, attribute.dataType.awsAttributeType)

      def toAwsKey(attribute: Attribute): Option[AwsKey] = attribute match {
        case key: Key => Some(new AwsKey(attribute.name, key.keyType.awsKeyType))
        case _        => None
      }
    }

    // TODO add secondary indexes

    sealed trait KeyType {
      def awsKeyType: AwsKeyType
      override def toString: String = awsKeyType.toString
    }

    case object PartitionKeyType extends KeyType {
      override def awsKeyType: AwsKeyType = AwsKeyType.HASH
    }

    case object SortKeyType extends KeyType {
      override def awsKeyType: AwsKeyType = AwsKeyType.RANGE
    }

    sealed trait DataType {
      def awsAttributeType: AwsAttributeType
      override def toString: String = awsAttributeType.toString
    }

    case object NumberDataType extends DataType {
      override def awsAttributeType = AwsAttributeType.N
    }

    case object StringDataType extends DataType {
      override def awsAttributeType = AwsAttributeType.S
    }

    case object BooleanDataType extends DataType {
      override def awsAttributeType = AwsAttributeType.B
    }

    object Provisioning {
      class Throughput(val maxReads: Long, val maxWrites: Long)
      def toAws(t: Throughput) = new AwsProvisionedThroughput(t.maxReads, t.maxWrites)
    }

    case class TableDescription(name: String)

    object TableDescription {
      def fromAws(awsTableDescr: AwsTableDescription): TableDescription =
        TableDescription(awsTableDescr.getTableName)
    }

  }

}
