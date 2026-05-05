package ru.socnetwork.mock

import org.apache.kafka.clients.producer.ProducerRecord
import ru.socnetwork.api.PostResponse
import ru.socnetwork.conf.ProducerConfig
import ru.socnetwork.kafka.KafkaProducer
import ru.socnetwork.kafka.KafkaProducerLive.dtoSerde
import zio.json.{DecoderOps, EncoderOps}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.{Task, ULayer, URLayer, ZIO, ZLayer}

case class KafkaProducerTest() extends KafkaProducer:

  override def produce(pr: PostResponse): Task[Unit] = ZIO.unit

object KafkaProducerTest:

  val layer: ULayer[KafkaProducer] =
    ZLayer.fromFunction(KafkaProducerTest.apply _)
