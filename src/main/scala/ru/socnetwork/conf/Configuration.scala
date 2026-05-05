package ru.socnetwork.conf

import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.redis.RedisConfig
import zio.{Config, Duration, Layer, ZLayer}

final case class RootConfig(
    config: AppConfig
)
final case class AppConfig(
    db: DbConfig,
    jwt: JwtConfig,
    redis: RedisConfig,
    consumerConfig: ConsumerConfig,
    producerConfig: ProducerConfig
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

final case class ConsumerConfig(
    topic: String,
    groupId: String,
    bootstrapServers: String,
    securityProtocol: String,
    enableAutoCommit: String,
    autoOffsetReset: String
)

final case class ProducerConfig(
    topic: String,
    bootstrapServers: String,
    securityProtocol: String,
    retries: Int,
    maxBlock: Duration,
    deliveryTimeout: Duration,
    requestTimeout: Duration
)

object Configuration:
  import zio.config.typesafe.*

  val layer: Layer[
    Config.Error,
    DbConfig
      with JwtConfig
      with RedisConfig
      with ConsumerConfig
      with ProducerConfig
  ] =
    for
      appConfig <- ZLayer.fromZIO(
        TypesafeConfigProvider
          .fromResourcePath()
          .load(deriveConfig[RootConfig])
          .map(_.config)
      )
      l <- ZLayer.succeed(appConfig.get.db) ++
        ZLayer.succeed(appConfig.get.jwt) ++
        ZLayer.succeed(appConfig.get.redis) ++
        ZLayer.succeed(appConfig.get.consumerConfig) ++
        ZLayer.succeed(appConfig.get.producerConfig)
    yield l
