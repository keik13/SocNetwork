package ru.socnetwork.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import ru.socnetwork.conf.{
  ConsumerConfig as MyConsumerConfig,
  ProducerConfig as MyProducerConfig
}
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.ProducerSettings
import zio.{URLayer, ZIO, ZLayer}

object KafkaSettings:

  val consumerSettingsLayer: URLayer[MyConsumerConfig, ConsumerSettings] =
    ZLayer.fromZIO(
      for
        config <- ZIO.service[MyConsumerConfig]
        consumerSettings <- ZIO.succeed(
          ConsumerSettings(
            config.bootstrapServers.split(",").map(_.trim).toList
          )
            .withGroupId(config.groupId)
            .withProperty(
              ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
              config.enableAutoCommit
            )
            .withProperty(
              ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
              config.autoOffsetReset
            )
            .withProperty(
              CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
              config.securityProtocol
            )
        )
      yield consumerSettings
    )

  val producerSettingsLayer: URLayer[MyProducerConfig, ProducerSettings] =
    ZLayer.fromZIO(
      for
        config <- ZIO.service[MyProducerConfig]
        producerSettings <- ZIO.succeed(
          ProducerSettings(
            config.bootstrapServers.split(",").map(_.trim).toList
          )
            .withProperties(
              CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> config.securityProtocol,
              ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG -> "false",
              ProducerConfig.MAX_BLOCK_MS_CONFIG -> config.maxBlock.toMillis.toInt.toString,
              ProducerConfig.RETRIES_CONFIG -> config.retries.toString,
              ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG -> config.deliveryTimeout.toMillis.toInt.toString,
              ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG -> config.requestTimeout.toMillis.toInt.toString
            )
        )
      yield producerSettings
    )
