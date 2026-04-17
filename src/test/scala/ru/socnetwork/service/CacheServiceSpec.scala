package ru.socnetwork.service

import ru.socnetwork.api.{
  PostCreateRequest,
  PostResponse,
  PostUpdateRequest,
  RegisterRequest
}
import ru.socnetwork.conf.{Configuration, JwtConfig}
import ru.socnetwork.containers.{Containers, DbMigrationAspect}
import ru.socnetwork.db.{Db, DbStrategy}
import ru.socnetwork.storage.{
  FriendshipStorageLive,
  PostStorageLive,
  UserStorage,
  UserStorageLive
}
import zio.redis.{CodecSupplier, Redis}
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, ProtobufCodec}
import zio.test.*
import zio.test.TestAspect.sequential
import zio.{ZEnvironment, ZIO, ZLayer}

import java.time.LocalDate

object CacheServiceSpec extends ZIOSpecDefault:

  object ProtobufCodecSupplier extends CodecSupplier:
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("CacheService")(
      test("should feed posts for user from cache") {
        for
          userService <- ZIO.service[UserService]
          friendshipService <- ZIO.service[FriendshipService]
          postService <- ZIO.service[PostService]
          cacheService <- ZIO.service[CacheService]
          user1 <- userService.register(user)
          friend <- userService.register(user)
          _ <- friendshipService.add(user1.userId, friend.userId)
          postIdFriend <- postService.add(
            PostCreateRequest("Друг запостил кринж"),
            friend.userId
          )
          postIdFriend <- postService.add(
            PostCreateRequest(
              "Друг запостил кринж2"
            ),
            friend.userId
          )
          postIdUser1 <- postService.add(
            PostCreateRequest(
              "Я запостил кринж, но его никто не увидит, у меня нет друзей"
            ),
            user1.userId
          )
          feedUser1 <- cacheService.feed(0, 10, user1.userId)
          feedFriend <- cacheService.feed(0, 10, friend.userId)
        yield assertTrue(feedUser1.size == 2 && feedFriend.isEmpty)
      },
      test("should feed posts for user from cache after rebuild") {
        for
          userService <- ZIO.service[UserService]
          friendshipService <- ZIO.service[FriendshipService]
          postService <- ZIO.service[PostService]
          cacheService <- ZIO.service[CacheService]
          user1 <- userService.register(user)
          friend <- userService.register(user)
          _ <- friendshipService.add(user1.userId, friend.userId)
          postIdFriend1 <- postService.add(
            PostCreateRequest("Друг запостил кринж"),
            friend.userId
          )
          postIdFriend2 <- postService.add(
            PostCreateRequest(
              "Друг запостил кринж2"
            ),
            friend.userId
          )
          postIdFriend3 <- postService.add(
            PostCreateRequest(
              "Друг запостил кринж3"
            ),
            friend.userId
          )
          postIdFriend4 <- postService.add(
            PostCreateRequest(
              "Друг запостил кринж4"
            ),
            friend.userId
          )
          postIdFriend5 <- postService.add(
            PostCreateRequest(
              "Друг запостил кринж5"
            ),
            friend.userId
          )
          postIdFriend6 <- postService.add(
            PostCreateRequest(
              "Друг запостил кринж6"
            ),
            friend.userId
          )
          postIdUser1 <- postService.add(
            PostCreateRequest(
              "Я запостил кринж, но его никто не увидит, у меня нет друзей"
            ),
            user1.userId
          )
          _ <- postService.delete(postIdFriend2, friend.userId)
          feedUser1 <- cacheService.feed(0, 10, user1.userId)
          feedFriend <- cacheService.feed(0, 10, friend.userId)
        yield assertTrue(feedUser1.size == 5 && feedFriend.isEmpty)
      }
    ) @@ DbMigrationAspect.migrateOnce()() @@ TestAspect.after(
      ZIO.serviceWithZIO[UserStorage](_.deleteAll())
    )
  }
    .provideShared(
      UserServiceLive.layer,
      UserStorageLive.layer,
      PostServiceLive.layer,
      PostStorageLive.layer,
      CacheServiceLive.layer,
      RebuildCacheServiceLive.layer,
      FriendshipStorageLive.layer,
      FriendshipServiceLive.layer,
      PasswordServiceLive.layer,
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
