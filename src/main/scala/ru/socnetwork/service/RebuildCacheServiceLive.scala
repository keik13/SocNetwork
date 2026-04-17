package ru.socnetwork.service

import ru.socnetwork.api.PostResponse
import ru.socnetwork.storage.{FriendshipStorage, PostStorage}
import zio.redis.Redis
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class RebuildCacheServiceLive(
    friendshipStorage: FriendshipStorage,
    postStorage: PostStorage,
    cacheService: CacheService
) extends RebuildCacheService:

  override def rebuildAllFollowerCache(): Task[Unit] =
    for
      followerIds <- friendshipStorage.getAllFollowers
      _ <- rebuild(followerIds)
    yield ()

  override def rebuildFollowerCache(userId: UUID): Task[Unit] =
    for
      followerIds <- friendshipStorage.getFollowers(userId)
      _ <- rebuild(followerIds)
    yield ()

  override def rebuildFriendCache(userId: UUID): Task[Unit] = postStorage
    .getFriendPosts(1000, userId)
    .flatMap(posts =>
      cacheService.rebuildCache(
        userId,
        posts.map(p => PostResponse(p.id, p.message, p.userId))
      )
    )

  private def rebuild(followerIds: List[UUID]) =
    ZIO.foreachParDiscard(followerIds) { rebuildFriendCache }

object RebuildCacheServiceLive:
  val layer: URLayer[
    FriendshipStorage & PostStorage & CacheService,
    RebuildCacheService
  ] =
    ZLayer.fromFunction(RebuildCacheServiceLive.apply _)
