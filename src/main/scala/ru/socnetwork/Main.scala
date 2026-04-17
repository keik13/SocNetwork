package ru.socnetwork

import ru.socnetwork.conf.Configuration
import ru.socnetwork.db.{Db, DbMigrator, DbStrategy}
import ru.socnetwork.server.{AuthMiddleware, SocNetworkServer}
import ru.socnetwork.service.{
  CacheServiceLive,
  CsvImportLive,
  FriendshipServiceLive,
  JwtServiceLive,
  PasswordServiceLive,
  PostServiceLive,
  RebuildCacheServiceLive,
  UserServiceLive
}
import ru.socnetwork.storage.{
  FriendshipStorageLive,
  PostStorageLive,
  UserStorageLive
}
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
        ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier)
      )
