package com.hbc.dockerit.matchers

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.hbc.dockerit.containers.DynamoDBContainer
import com.hbc.dockerit.util.DynamoDBUtil
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

trait DynamoDBMatchers extends Matchers {
  container: DynamoDBContainer =>

  def beAvailable() = Matcher { client: DynamoDB =>
    MatchResult(DynamoDBUtil(client).ping,
                s"""DynamoDB does not appear to be available""",
                s"""DynamoDB is available""")

  }

  def haveTable(tableName: String) = Matcher { client: DynamoDB =>
    DynamoDBUtil(client).tableExists(tableName) match {

      case Right(tableExists) =>
        MatchResult(tableExists,
                    s"""Expected table "$tableName" to exist but it does not""",
                    s"""Did not expect table "$tableName" to exist""")

      case Left(e) =>
        val msg = s"""Error looking up table $tableName: $e"""
        MatchResult(matches = false, msg, msg)
    }

  }

  def haveItemCount(tableName: String, expected: Long) = Matcher { client: DynamoDB =>
    DynamoDBUtil(client).getItemCount(tableName) match {

      case Right(actual) =>
        MatchResult(actual == expected,
                    s"""Table "$tableName" has unexpected itemCount $actual (expected $expected)""",
                    s"""Table "$tableName" not expected to have itemCount $expected but it did""")

      case Left(e) =>
        val msg = s"""Error looking up table $tableName: $e"""
        MatchResult(matches = false, msg, msg)
    }

  }

}
