package ru.socnetwork.service

import ru.socnetwork.api.JwtClaims
import zio.*

trait JwtService:
  def generate(claims: JwtClaims): Task[String]
  def verify(token: String): Task[JwtClaims]
