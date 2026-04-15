package ru.socnetwork.service

import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtZIOJson}
import ru.socnetwork.api.UserInfo
import ru.socnetwork.conf.JwtConfig
import ru.socnetwork.util.InvalidToken
import zio.json.{DecoderOps, EncoderOps}
import zio.{Clock, Task, URLayer, ZIO, ZLayer}

import scala.concurrent.duration.SECONDS

final case class JwtServiceLive(jwtConfig: JwtConfig) extends JwtService:

  override def generate(claims: UserInfo): Task[String] =
    for
      ct <- Clock.currentTime(SECONDS)
      claim <- ZIO.attempt(
        JwtClaim(
          content = claims.toJson,
          issuer = Some(jwtConfig.issuer),
          issuedAt = Some(ct),
          expiration = Some(ct + jwtConfig.expireInSeconds)
        )
      )
    yield JwtZIOJson.encode(claim, jwtConfig.secret, JwtAlgorithm.HS256)

  override def verify(token: String): Task[UserInfo] =
    for
      jwt <- ZIO.fromTry(
        JwtZIOJson.decode(token, jwtConfig.secret, Seq(JwtAlgorithm.HS256))
      )
      r <- ZIO
        .from(jwt.content.fromJson[UserInfo])
        .tapError(err => ZIO.logError(err))
        .orElseFail(InvalidToken)
    yield r

object JwtServiceLive:
  val layer: URLayer[JwtConfig, JwtService] =
    ZLayer.fromFunction(JwtServiceLive.apply _)
