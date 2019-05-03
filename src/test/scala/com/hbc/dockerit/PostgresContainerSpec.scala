package com.hbc.dockerit

import java.sql.{ DriverManager, Statement }

import com.hbc.dockerit.containers.PostgresContainer
import org.scalatest.WordSpec

class PostgresContainerSpec extends WordSpec with DockerSuite with PostgresContainer {

  def compare: AfterWord = afterWord("compare")

  "PostgresContainer" should {

    def newConnection = DriverManager.getConnection(Postgres.url, Postgres.user, Postgres.password)

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
          rs.next()    should be(true)
          rs.getInt(1) should be(1)
        })
      }
    }
  }

}
