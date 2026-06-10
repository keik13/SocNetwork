package ru.socnetwork.mock

import ru.socnetwork.kafka.KafkaConsumer
import zio.stream.ZStream
import zio.{ULayer, ZLayer}

case class KafkaConsumerTest() extends KafkaConsumer:

  override def consume: ZStream[Any, Throwable, Unit] = ZStream.unit

object KafkaConsumerTest:

  val layer: ULayer[KafkaConsumer] =
    ZLayer.fromFunction(KafkaConsumerTest.apply _)
