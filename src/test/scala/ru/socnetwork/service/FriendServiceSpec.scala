package ru.socnetwork.service

import ru.socnetwork.api.RegisterRequest
import ru.socnetwork.conf.{Configuration, JwtConfig}
import ru.socnetwork.containers.{Containers, DbMigrationAspect}
import ru.socnetwork.db.{Db, DbStrategy}
import ru.socnetwork.service.PostServiceSpec.ProtobufCodecSupplier
import ru.socnetwork.storage.{
  FriendshipStorageLive,
  PostStorageLive,
  UserStorage,
  UserStorageLive
}
import zio.redis.{CodecSupplier, Redis}
import zio.test.*
import zio.test.TestAspect.sequential
import zio.{ZEnvironment, ZIO, ZLayer}

import java.time.LocalDate

object FriendServiceSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("FiendshipService")(
      test("should add friend to user") {
        for
          userService <- ZIO.service[UserService]
          friendshipService <- ZIO.service[FriendshipService]
          user1 <- userService.register(user)
          friendUser <- userService.register(user)
          _ <- friendshipService.add(user1.userId, friendUser.userId)
        yield assertTrue(true)
      },
      test("should delete friend from user") {
        for
          userService <- ZIO.service[UserService]
          friendshipService <- ZIO.service[FriendshipService]
          user1 <- userService.register(user)
          friendUser <- userService.register(user)
          _ <- friendshipService.add(user1.userId, friendUser.userId)
          _ <- friendshipService.delete(user1.userId, friendUser.userId)
        yield assertTrue(true)
      }
    ) @@ DbMigrationAspect.migrateOnce()() @@ TestAspect.after(
      ZIO.serviceWithZIO[UserStorage](_.deleteAll())
    )
  }
    .provideShared(
      UserServiceLive.layer,
      UserStorageLive.layer,
      FriendshipStorageLive.layer,
      FriendshipServiceLive.layer,
      PasswordServiceLive.layer,
      RebuildCacheServiceLive.layer,
      PostStorageLive.layer,
      CacheServiceLive.layer,
      JwtServiceLive.layer,
      Configuration.layer.map(c => ZEnvironment(c.get[JwtConfig])),
      Containers.layer,
      Db.dataSourceLayer,
      Db.quillMasterLayer,
      Db.quillSlaveLayer,
      DbStrategy.layer,
      Containers.postgresLayer,
      Containers.redisLayer,
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
      Redis.singleNode
    ) @@ sequential

  val user = RegisterRequest(
    "Vasya",
    "Driszt",
    LocalDate.of(2000, 1, 1),
    "Very bored man",
    "Gvatemala",
    "12345G"
  )
