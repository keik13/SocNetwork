package ru.socnetwork.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import ru.socnetwork.api.PostResponse
import ru.socnetwork.conf.ProducerConfig
import ru.socnetwork.kafka.KafkaProducerLive.dtoSerde
import zio.json.{DecoderOps, EncoderOps}
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde

case class KafkaProducerLive(producer: Producer, producerConfig: ProducerConfig)
    extends KafkaProducer:

  override def produce(pr: PostResponse): Task[Unit] = producer
    .produce[Any, Nothing, PostResponse](
      new ProducerRecord(producerConfig.topic, pr),
      Serde.string,
      dtoSerde
    )
    .unit

object KafkaProducerLive:

  val layer: URLayer[
    Producer & ProducerConfig,
    KafkaProducer
  ] =
    ZLayer.fromFunction(KafkaProducerLive.apply _)

  val dtoSerde: Serde[Any, PostResponse] =
    Serde.string.inmapZIO[Any, PostResponse](s =>
      ZIO
        .fromEither(s.fromJson[PostResponse])
        .tapError(err => ZIO.logError(err))
        .mapError(new RuntimeException(_))
    )(dto => ZIO.attempt(dto.toJson))
