package ru.socnetwork

import ru.socnetwork.Main.Environment
import ru.socnetwork.auth.JwtServiceLive
import ru.socnetwork.conf.Configuration
import ru.socnetwork.db.{Db, DbMigrator, DbStrategy}
import ru.socnetwork.kafka.{KafkaConsumerLive, KafkaProducerLive, KafkaSettings}
import ru.socnetwork.server.{AuthMiddleware, SocNetworkServer}
import ru.socnetwork.service.*
import ru.socnetwork.storage.{
  FriendshipStorageLive,
  PostStorageLive,
  UserStorageLive
}
import zio.kafka.consumer.Consumer
import zio.kafka.producer.Producer
import zio.redis.{CodecSupplier, Redis}
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, ProtobufCodec}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object Main extends ZIOAppDefault:

  object ProtobufCodecSupplier extends CodecSupplier:
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec

  override val run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[SocNetworkServer](_.start)
      .provide(
        Configuration.layer,
        Db.dataSourceLayer,
        Db.quillMasterLayer,
        Db.quillSlaveLayer,
        DbStrategy.layer,
        DbMigrator.layer,
        SocNetworkServer.layer,
        UserServiceLive.layer,
        UserStorageLive.layer,
        PasswordServiceLive.layer,
        JwtServiceLive.layer,
        CsvImportLive.layer,
        FriendshipServiceLive.layer,
        FriendshipStorageLive.layer,
        PostServiceLive.layer,
        PostStorageLive.layer,
        CacheServiceLive.layer,
        RebuildCacheServiceLive.layer,
        AuthMiddleware.layer,
        Redis.singleNode,
        ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
        Producer.live,
        KafkaSettings.producerSettingsLayer,
        ConnectionServiceLive.layer,
        KafkaProducerLive.layer,
        KafkaConsumerLive.layer,
        Consumer.live,
        KafkaSettings.consumerSettingsLayer
      )

//object KafkaWSNotifier extends ZIOAppDefault:
//  override val run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
//    ZIO
//      .serviceWithZIO[KafkaConsumer](_.consume.runDrain)
//      .provide(
//        KafkaConsumerLive.layer,
//        Consumer.live,
//        KafkaSettings.consumerSettingsLayer,
//        Configuration.layer,
//        ConnectionServiceLive.layer,
//        FriendshipServiceLive.layer,
//        FriendshipStorageLive.layer,
//        RebuildCacheServiceLive.layer,
//        PostStorageLive.layer,
//        CacheServiceLive.layer,
//        Db.dataSourceLayer,
//        Db.quillMasterLayer,
//        Db.quillSlaveLayer,
//        DbStrategy.layer,
//        Redis.singleNode,
//        ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier)
//      )

//object Main extends ZIOApp.Proxy(SocNetwork <> KafkaWSNotifier)
