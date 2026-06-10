package ru.socnetwork.mock

import ru.socnetwork.api.PostResponse
import ru.socnetwork.kafka.KafkaProducer
import zio.{Task, ULayer, ZIO, ZLayer}

case class KafkaProducerTest() extends KafkaProducer:

  override def produce(pr: PostResponse): Task[Unit] = ZIO.unit

object KafkaProducerTest:

  val layer: ULayer[KafkaProducer] =
    ZLayer.fromFunction(KafkaProducerTest.apply _)
