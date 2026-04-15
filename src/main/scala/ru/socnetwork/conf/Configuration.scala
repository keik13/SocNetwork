package ru.socnetwork.conf

import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.redis.RedisConfig
import zio.{Config, Layer, ZLayer}

final case class RootConfig(
    config: AppConfig
)
final case class AppConfig(
    db: DbConfig,
    jwt: JwtConfig,
    redis: RedisConfig
)

final case class DbConfig(
    urlMaster: String,
    urlSlave: String,
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

  val layer: Layer[Config.Error, DbConfig with JwtConfig with RedisConfig] =
    for
      appConfig <- ZLayer.fromZIO(
        TypesafeConfigProvider
          .fromResourcePath()
          .load(deriveConfig[RootConfig])
          .map(_.config)
      )
      l <- ZLayer.succeed(appConfig.get.db) ++
        ZLayer.succeed(appConfig.get.jwt) ++
        ZLayer.succeed(appConfig.get.redis)
    yield l
