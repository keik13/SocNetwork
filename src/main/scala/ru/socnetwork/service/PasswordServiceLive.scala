package ru.socnetwork.service

import com.password4j.{Password, SecureString}
import zio.{Task, ULayer, ZIO, ZLayer}

final case class PasswordServiceLive() extends PasswordService:
  override def hashPassword(plain: String): Task[String] = ZIO
    .attemptBlocking {
      val secure = new SecureString(plain.toCharArray)
      try
        Password.hash(secure).withBcrypt().getResult
      finally
        secure.clear()
    }

  override def verifyPassword(plain: String, hash: String): Task[Boolean] = ZIO
    .attemptBlocking {
      val secure = new SecureString(plain.toCharArray)
      try
        Password.check(secure, hash).withBcrypt()
      finally
        secure.clear()
    }

object PasswordServiceLive:
  val layer: ULayer[PasswordServiceLive] =
    ZLayer.fromFunction(PasswordServiceLive.apply _)
