package ru.socnetwork.service

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtZIOJson}
import ru.socnetwork.api.JwtClaims
import ru.socnetwork.conf.JwtConfig
import ru.socnetwork.util.InvalidBody
import zio.json.{DecoderOps, EncoderOps}
import zio.{Clock, Task, URLayer, ZIO, ZLayer}

import scala.concurrent.duration.SECONDS

final case class JwtServiceLive(jwtConfig: JwtConfig) extends JwtService:

  override def generate(claims: JwtClaims): Task[String] =
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

  override def verify(token: String): Task[JwtClaims] =
    for
      jwt <- ZIO.fromTry(
        JwtZIOJson.decode(token, jwtConfig.secret, Seq(JwtAlgorithm.HS256))
      )
      r <- ZIO
        .from(jwt.content.fromJson[JwtClaims])
        .tapError(err => ZIO.logError(err))
        .orElseFail(InvalidBody)
    yield r

object JwtServiceLive:
  val layer: URLayer[JwtConfig, JwtService] =
    ZLayer.fromFunction(JwtServiceLive.apply _)
