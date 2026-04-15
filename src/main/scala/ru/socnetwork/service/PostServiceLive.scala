package ru.socnetwork.service

import ru.socnetwork.api.{PostCreateRequest, PostResponse, PostUpdateRequest}
import ru.socnetwork.storage.{FriendshipStorage, PostRow, PostStorage}
import zio.{Chunk, Clock, Random, Task, URLayer, ZIO, ZLayer}
import zio.redis.*

import java.util.UUID
import scala.concurrent.duration.SECONDS

final case class PostServiceLive(
    postStorage: PostStorage,
    friendshipStorage: FriendshipStorage,
    redis: Redis
) extends PostService:

  override def add(request: PostCreateRequest, userId: UUID): Task[UUID] =
    for
      ct <- Clock.currentTime(SECONDS)
      uuid <- Random.nextUUID
      _ <- postStorage.add(PostRow(uuid, userId, request.text, ct, ct))
      _ <- updateCache(request.text, userId, uuid)
    yield uuid

  private def updateCache(text: String, userId: UUID, uuid: UUID) =
    for
      friendIds <- friendshipStorage.getFollowers(userId)
      _ <- ZIO.foreachParDiscard(friendIds) { friendId =>
        redis
          .lPush(
            s"feed:user:$friendId",
            PostResponse(uuid, text, userId)
          )
          *> redis.lTrim(s"feed:user:$friendId", 0 until 1000)
      }
    yield ()

  override def update(update: PostUpdateRequest, userId: UUID): Task[Unit] =
    for
      ct <- Clock.currentTime(SECONDS)
      _ <- postStorage.update(update.id, update.text, userId, ct)
      _ <- updateCache(update.text, userId, update.id)
    yield ()

  override def getById(id: UUID): Task[Option[PostResponse]] =
    (for row <- postStorage.getById(id).some
    yield PostResponse(row.id, row.message, row.userId)).unsome

  override def delete(id: UUID, userId: UUID): Task[Unit] =
    postStorage.delete(id, userId)

  override def feed(
      offset: Int,
      limit: Int,
      userId: UUID
  ): Task[Chunk[PostResponse]] =
    redis
      .lRange(s"feed:user:$userId", Range(offset, limit))
      .returning[PostResponse]

object PostServiceLive:
  val layer: URLayer[PostStorage & Redis & FriendshipStorage, PostService] =
    ZLayer.fromFunction(PostServiceLive.apply _)
