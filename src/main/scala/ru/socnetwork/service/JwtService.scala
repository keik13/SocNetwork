package ru.socnetwork.service

import ru.socnetwork.api.UserInfo
import zio.*

trait JwtService:
  def generate(claims: UserInfo): Task[String]
  def verify(token: String): Task[UserInfo]
