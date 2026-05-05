package ru.socnetwork.service

import ru.socnetwork.api.{PostCreateRequest, PostResponse, PostUpdateRequest}
import ru.socnetwork.kafka.KafkaProducer
import ru.socnetwork.storage.{PostRow, PostStorage}
import zio.{Clock, Random, Task, URLayer, ZIO, ZLayer}

import java.util.UUID
import scala.concurrent.duration.SECONDS

final case class PostServiceLive(
    postStorage: PostStorage,
    friendshipService: FriendshipService,
    cacheService: CacheService,
    rebuildCacheService: RebuildCacheService,
    producer: KafkaProducer
) extends PostService:

  override def add(request: PostCreateRequest, userId: UUID): Task[UUID] =
    for
      ct <- Clock.currentTime(SECONDS)
      uuid <- Random.nextUUID
      _ <- postStorage.add(PostRow(uuid, userId, request.text, ct, ct))
      followerIds <- friendshipService.getFollowers(userId)
      pr <- ZIO.succeed(PostResponse(uuid, request.text, userId))
      _ <- cacheService.updateCache(pr, followerIds)
      _ <- producer.produce(pr)
    yield uuid

  override def update(update: PostUpdateRequest, userId: UUID): Task[Unit] =
    for
      ct <- Clock.currentTime(SECONDS)
      _ <- postStorage.update(update.id, update.text, userId, ct)
      followerIds <- friendshipService.getFollowers(userId)
      pr <- ZIO.succeed(PostResponse(update.id, update.text, userId))
      _ <- cacheService.updateCache(pr, followerIds)
      _ <- producer.produce(pr)
    yield ()

  override def getById(id: UUID): Task[Option[PostResponse]] =
    (for row <- postStorage.getById(id).some
    yield PostResponse(row.id, row.message, row.userId)).unsome

  override def delete(id: UUID, userId: UUID): Task[Unit] =
    for
      _ <- postStorage.delete(id, userId)
      _ <- rebuildCacheService.rebuildFollowerCache(userId)
    yield ()

  override def getFriendPosts(
      count: Int,
      userId: UUID
  ): Task[List[PostResponse]] = postStorage
    .getFriendPosts(count, userId)
    .map(pr => pr.map(p => PostResponse(p.id, p.message, p.userId)))

object PostServiceLive:
  val layer: URLayer[
    PostStorage & CacheService & FriendshipService & RebuildCacheService &
      KafkaProducer,
    PostService
  ] =
    ZLayer.fromFunction(PostServiceLive.apply _)
