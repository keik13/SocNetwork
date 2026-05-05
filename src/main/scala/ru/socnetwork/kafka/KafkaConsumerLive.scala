package ru.socnetwork.kafka

import ru.socnetwork.api.PostResponse
import ru.socnetwork.conf.ConsumerConfig
import ru.socnetwork.kafka.KafkaProducerLive.dtoSerde
import ru.socnetwork.service.{ConnectionService, FriendshipService}
import zio.http.ChannelEvent.Read
import zio.http.WebSocketFrame.Text
import zio.json.EncoderOps
import zio.kafka.consumer.*
import zio.kafka.serde.Serde
import zio.stream.{ZSink, ZStream}
import zio.{Duration, Schedule, URLayer, ZIO, ZLayer}

import java.util.concurrent.TimeUnit.SECONDS

final case class KafkaConsumerLive(
    consumer: Consumer,
    consumerConfig: ConsumerConfig,
    friendshipService: FriendshipService,
    connectionService: ConnectionService
) extends KafkaConsumer:

  override def consume: ZStream[Any, Throwable, Unit] =
    consumer
      .plainStream(
        Subscription.topics(consumerConfig.topic),
        Serde.string,
        dtoSerde
      )
      .aggregateAsync(
        ZSink.collectAllN[CommittableRecord[String, PostResponse]](10)
      )
      .mapZIO { batch =>
        for
          _ <- ZIO.foreachDiscard(batch)(pr =>
            for
              followers <- friendshipService.getFollowers(pr.value.authorUserId)
              channels <- ZIO
                .foreach(followers)(f => connectionService.getChannels(f))
              _ <- ZIO.foreachDiscard(channels.flatten)(c =>
                c.send(Read(Text(pr.value.toJson)))
              )
            yield ()
          )
          offsetBatch = batch.foldLeft(OffsetBatch.empty) { case (accum, cr) =>
            accum add cr.offset
          }
          _ <- offsetBatch.commitOrRetry(
            Schedule.spaced(Duration(5, SECONDS)) && Schedule.recurs(10)
          )
        yield ()
      }

object KafkaConsumerLive:

  val layer: URLayer[
    Consumer with ConsumerConfig with ConnectionService with FriendshipService,
    KafkaConsumerLive
  ] =
    ZLayer.fromFunction(KafkaConsumerLive.apply _)
