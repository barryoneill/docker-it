package com.hbc.dockerit.models

import io.circe.{Decoder, Encoder}

case class BankAccount(accountNumber: String, amount: Double)

object BankAccount {

  implicit val decoder: Decoder[BankAccount] = Decoder.forProduct2("account_number", "amount")(BankAccount.apply)

  implicit val encode: Encoder[BankAccount] = Encoder.forProduct2("account_number", "amount")(ba => (ba.accountNumber, ba.amount))

}