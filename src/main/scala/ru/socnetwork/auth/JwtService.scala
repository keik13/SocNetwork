package ru.socnetwork.auth

import zio.*

trait JwtService:
  def generate(claims: UserInfo): Task[String]
  def verify(token: String): Task[UserInfo]
