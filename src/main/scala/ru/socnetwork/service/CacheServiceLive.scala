package ru.socnetwork.service

import ru.socnetwork.api.PostResponse
import zio.redis.Redis
import zio.{Chunk, Task, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class CacheServiceLive(
    redis: Redis
) extends CacheService:

  override def updateCache(
      text: String,
      userId: UUID,
      uuid: UUID,
      followerIds: List[UUID]
  ): Task[Unit] =
    for _ <- ZIO.foreachParDiscard(followerIds) { followerId =>
        redis
          .lPush(
            s"feed:user:$followerId",
            PostResponse(uuid, text, userId)
          )
          *> redis.lTrim(s"feed:user:$followerId", 0 until 1000)
      }
    yield ()

  override def rebuildCache(
      userId: UUID,
      postResp: List[PostResponse]
  ): Task[Unit] =
    for
      _ <- redis.del(s"feed:user:$userId")
      _ <- ZIO.when(postResp.nonEmpty)(
        redis.lPush(s"feed:user:$userId", postResp.head, postResp.tail*)
      )
    yield ()

  override def feed(
      offset: Int,
      limit: Int,
      userId: UUID
  ): Task[Chunk[PostResponse]] =
    redis
      .lRange(s"feed:user:$userId", Range(offset, limit))
      .returning[PostResponse]

object CacheServiceLive:
  val layer: URLayer[Redis, CacheService] =
    ZLayer.fromFunction(CacheServiceLive.apply _)
