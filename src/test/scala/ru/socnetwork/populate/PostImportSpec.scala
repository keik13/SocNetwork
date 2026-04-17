package ru.socnetwork.populate

import ru.socnetwork.api.PostCreateRequest
import ru.socnetwork.conf.{Configuration, JwtConfig}
import ru.socnetwork.containers.{Containers, DbMigrationAspect}
import ru.socnetwork.db.{Db, DbStrategy}
import ru.socnetwork.service.{
  CacheServiceLive,
  FriendshipService,
  FriendshipServiceLive,
  JwtServiceLive,
  PasswordServiceLive,
  PostService,
  PostServiceLive,
  RebuildCacheServiceLive,
  UserService,
  UserServiceLive
}
import ru.socnetwork.storage.{
  FriendshipStorageLive,
  PostStorageLive,
  UserStorage,
  UserStorageLive
}
import zio.redis.{CodecSupplier, Redis}
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, ProtobufCodec}
import zio.stream.{ZPipeline, ZStream}
import zio.test.*
import zio.test.TestAspect.{ignore, sequential}
import zio.{ZEnvironment, ZIO, ZLayer}

import java.util.UUID

object PostImportSpec extends ZIOSpecDefault:

  object ProtobufCodecSupplier extends CodecSupplier:
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("PostPopulate")(
      test("should add posts for users") {
        for
          userService <- ZIO.service[UserService]
          friendshipService <- ZIO.service[FriendshipService]
          postService <- ZIO.service[PostService]
          _ <- friendshipService.add(
            UUID.fromString("527215ad-34dc-443e-ac37-2623cd77288b"),
            UUID.fromString("5a8e7981-fbe5-46bc-b719-af6e7961d5ec")
          )
          _ <- ZStream
            .fromFileName("posts")
            // Декодируем байты в UTF-8 строки
            .via(ZPipeline.utf8Decode)
            // Разделяем по двойному переносу строки
            .via(ZPipeline.splitOn("\n"))
            // Убираем лишние пробелы по краям каждого абзаца (опционально)
            .map(_.trim)
            // Фильтруем пустые элементы
            .filter(_.nonEmpty)
            .foreach(str =>
              postService.add(
                PostCreateRequest(str),
                UUID.fromString("5a8e7981-fbe5-46bc-b719-af6e7961d5ec")
              )
            )
        yield assertTrue(true)
      }
    ) @@ DbMigrationAspect.migrateOnce()() @@ ignore
  }
    .provideShared(
      UserServiceLive.layer,
      UserStorageLive.layer,
      PostServiceLive.layer,
      PostStorageLive.layer,
      FriendshipServiceLive.layer,
      FriendshipStorageLive.layer,
      CacheServiceLive.layer,
      RebuildCacheServiceLive.layer,
      PasswordServiceLive.layer,
      JwtServiceLive.layer,
      Configuration.layer.map(c => ZEnvironment(c.get[JwtConfig])),
      Containers.layer,
      Containers.postgresLayer,
      Containers.redisLayer,
      Db.dataSourceLayer,
      Db.quillMasterLayer,
      Db.quillSlaveLayer,
      DbStrategy.layer,
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
      Redis.singleNode
    ) @@ sequential
