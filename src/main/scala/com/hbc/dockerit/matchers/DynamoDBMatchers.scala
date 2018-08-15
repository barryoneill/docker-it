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
                          s"""Table: "$tableName" exists""")

            case Left(e) =>
              val msg = s"""Error looking up table $tableName: $e"""
              MatchResult(matches = false, msg, msg)
    }

  }

}
