package ru.socnetwork.service

import com.password4j.{Password, Hash, SecureString}
import zio.*

trait PasswordService:
  def hashPassword(plain: String): Task[String]
  def verifyPassword(plain: String, hash: String): Task[Boolean]
