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

object PostServiceSpec extends ZIOSpecDefault:

  object ProtobufCodecSupplier extends CodecSupplier:
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("PostService")(
      test("should add post to user") {
        for
          userService <- ZIO.service[UserService]
          postService <- ZIO.service[PostService]
          user1 <- userService.register(user)
          postId <- postService.add(
            PostCreateRequest(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lectus mauris ultrices eros in cursus turpis massa. In fermentum et sollicitudin ac orci. Faucibus ornare suspendisse sed nisi lacus sed. Vivamus at augue eget arcu dictum varius. Lobortis scelerisque fermentum dui faucibus in ornare quam. Feugiat in fermentum posuere urna nec tincidunt. Ullamcorper velit sed ullamcorper morbi tincidunt ornare. In hac habitasse platea dictumst quisque sagittis. Felis bibendum ut tristique et. Diam maecenas ultricies mi eget mauris pharetra et ultrices neque.\nSapien nec sagittis aliquam malesuada bibendum arcu vitae. Quis ipsum suspendisse ultrices gravida dictum fusce. Nunc mattis enim ut tellus elementum. Quis imperdiet massa tincidunt nunc pulvinar sapien et ligula ullamcorper. Amet consectetur adipiscing elit pellentesque. Nibh ipsum consequat nisl vel pretium lectus quam id. Et tortor at risus viverra adipiscing at. Nibh tortor id aliquet lectus proin nibh nisl condimentum. Amet justo donec enim diam vulputate ut pharetra sit. Egestas pretium aenean pharetra magna ac. Amet luctus venenatis lectus magna fringilla urna porttitor rhoncus dolor. Aliquam sem et tortor consequat id porta. Dictum varius duis at consectetur lorem donec massa. Molestie a iaculis at erat pellentesque adipiscing commodo elit. Accumsan lacus vel facilisis volutpat est velit egestas."
            ),
            user1.userId
          )
        yield assertTrue(postId.toString.nonEmpty)
      },
      test("should update post from user") {
        for
          userService <- ZIO.service[UserService]
          postService <- ZIO.service[PostService]
          user1 <- userService.register(user)
          postId <- postService.add(
            PostCreateRequest(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lectus mauris ultrices eros in cursus turpis massa. In fermentum et sollicitudin ac orci. Faucibus ornare suspendisse sed nisi lacus sed. Vivamus at augue eget arcu dictum varius. Lobortis scelerisque fermentum dui faucibus in ornare quam. Feugiat in fermentum posuere urna nec tincidunt. Ullamcorper velit sed ullamcorper morbi tincidunt ornare. In hac habitasse platea dictumst quisque sagittis. Felis bibendum ut tristique et. Diam maecenas ultricies mi eget mauris pharetra et ultrices neque.\nSapien nec sagittis aliquam malesuada bibendum arcu vitae. Quis ipsum suspendisse ultrices gravida dictum fusce. Nunc mattis enim ut tellus elementum. Quis imperdiet massa tincidunt nunc pulvinar sapien et ligula ullamcorper. Amet consectetur adipiscing elit pellentesque. Nibh ipsum consequat nisl vel pretium lectus quam id. Et tortor at risus viverra adipiscing at. Nibh tortor id aliquet lectus proin nibh nisl condimentum. Amet justo donec enim diam vulputate ut pharetra sit. Egestas pretium aenean pharetra magna ac. Amet luctus venenatis lectus magna fringilla urna porttitor rhoncus dolor. Aliquam sem et tortor consequat id porta. Dictum varius duis at consectetur lorem donec massa. Molestie a iaculis at erat pellentesque adipiscing commodo elit. Accumsan lacus vel facilisis volutpat est velit egestas."
            ),
            user1.userId
          )
          _ <- postService.update(
            PostUpdateRequest(
              postId,
              "Nulla at volutpat diam ut. Sit amet tellus cras adipiscing enim. Eu consequat ac felis donec et odio pellentesque diam. Viverra adipiscing at in tellus integer feugiat scelerisque varius morbi. Elementum pulvinar etiam non quam lacus suspendisse faucibus. Fames ac turpis egestas maecenas pharetra convallis posuere. Massa enim nec dui nunc. Quis ipsum suspendisse ultrices gravida dictum fusce ut placerat. Condimentum lacinia quis vel eros donec ac odio tempor. Donec adipiscing tristique risus nec. Morbi non arcu risus quis varius quam quisque id diam. Id cursus metus aliquam eleifend mi in nulla posuere sollicitudin. Tempor orci eu lobortis elementum. Integer malesuada nunc vel risus commodo viverra."
            ),
            user1.userId
          )
          updatedPost <- postService.getById(postId)
        yield assertTrue(
          updatedPost.get == PostResponse(
            postId,
            "Nulla at volutpat diam ut. Sit amet tellus cras adipiscing enim. Eu consequat ac felis donec et odio pellentesque diam. Viverra adipiscing at in tellus integer feugiat scelerisque varius morbi. Elementum pulvinar etiam non quam lacus suspendisse faucibus. Fames ac turpis egestas maecenas pharetra convallis posuere. Massa enim nec dui nunc. Quis ipsum suspendisse ultrices gravida dictum fusce ut placerat. Condimentum lacinia quis vel eros donec ac odio tempor. Donec adipiscing tristique risus nec. Morbi non arcu risus quis varius quam quisque id diam. Id cursus metus aliquam eleifend mi in nulla posuere sollicitudin. Tempor orci eu lobortis elementum. Integer malesuada nunc vel risus commodo viverra.",
            user1.userId
          )
        )
      },
      test("should delete post from user") {
        for
          userService <- ZIO.service[UserService]
          postService <- ZIO.service[PostService]
          user1 <- userService.register(user)
          postId <- postService.add(
            PostCreateRequest(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Lectus mauris ultrices eros in cursus turpis massa. In fermentum et sollicitudin ac orci. Faucibus ornare suspendisse sed nisi lacus sed. Vivamus at augue eget arcu dictum varius. Lobortis scelerisque fermentum dui faucibus in ornare quam. Feugiat in fermentum posuere urna nec tincidunt. Ullamcorper velit sed ullamcorper morbi tincidunt ornare. In hac habitasse platea dictumst quisque sagittis. Felis bibendum ut tristique et. Diam maecenas ultricies mi eget mauris pharetra et ultrices neque.\nSapien nec sagittis aliquam malesuada bibendum arcu vitae. Quis ipsum suspendisse ultrices gravida dictum fusce. Nunc mattis enim ut tellus elementum. Quis imperdiet massa tincidunt nunc pulvinar sapien et ligula ullamcorper. Amet consectetur adipiscing elit pellentesque. Nibh ipsum consequat nisl vel pretium lectus quam id. Et tortor at risus viverra adipiscing at. Nibh tortor id aliquet lectus proin nibh nisl condimentum. Amet justo donec enim diam vulputate ut pharetra sit. Egestas pretium aenean pharetra magna ac. Amet luctus venenatis lectus magna fringilla urna porttitor rhoncus dolor. Aliquam sem et tortor consequat id porta. Dictum varius duis at consectetur lorem donec massa. Molestie a iaculis at erat pellentesque adipiscing commodo elit. Accumsan lacus vel facilisis volutpat est velit egestas."
            ),
            user1.userId
          )
          _ <- postService.delete(postId, user1.userId)
          deletedPost <- postService.getById(postId)
        yield assertTrue(deletedPost.isEmpty)
      },
      test("should feed post from user") {
        for
          userService <- ZIO.service[UserService]
          friendshipService <- ZIO.service[FriendshipService]
          postService <- ZIO.service[PostService]
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
          feedUser1 <- postService.getFriendPosts(10, user1.userId)
          feedFriend <- postService.getFriendPosts(10, friend.userId)
        yield assertTrue(feedUser1.size == 2 && feedFriend.isEmpty)
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
      FriendshipServiceLive.layer,
      FriendshipStorageLive.layer,
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
