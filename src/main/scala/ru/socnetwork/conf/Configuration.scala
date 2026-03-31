package ru.socnetwork.conf

import zio.Config.Error.InvalidData
import zio.config.magnolia.{deriveConfig, DeriveConfig}
import zio.http.URL
import zio.{Config, Layer, ZLayer}

import scala.concurrent.duration.FiniteDuration

final case class RootConfig(
    config: AppConfig
)
final case class AppConfig(
    db: DbConfig,
    jwt: JwtConfig
)

final case class DbConfig(
    url: String,
    user: String,
    password: String
)

final case class JwtConfig(
    secret: String,
    issuer: String,
    expireInSeconds: Int
)

object Configuration:
  import zio.config.typesafe.*

  val layer: Layer[Config.Error, DbConfig with JwtConfig] =
    for
      appConfig <- ZLayer.fromZIO(
        TypesafeConfigProvider
          .fromResourcePath()
          .load(deriveConfig[RootConfig])
          .map(_.config)
      )
      l <- ZLayer.succeed(appConfig.get.db) ++ ZLayer.succeed(appConfig.get.jwt)
    yield l
