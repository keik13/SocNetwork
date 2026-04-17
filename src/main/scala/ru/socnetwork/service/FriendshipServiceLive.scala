package ru.socnetwork.service

import ru.socnetwork.storage.FriendshipStorage
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class FriendshipServiceLive(
    friendshipStorage: FriendshipStorage,
    rebuildCacheService: RebuildCacheService
) extends FriendshipService:

  override def add(userId: UUID, friendUserId: UUID): Task[Unit] =
    friendshipStorage.add(userId, friendUserId)

  override def delete(userId: UUID, friendUserId: UUID): Task[Unit] =
    for
      _ <- friendshipStorage.delete(userId, friendUserId)
      followerIds <- getFollowers(userId)
      _ <- rebuildCacheService.rebuildFriendCache(userId)
    yield ()

  override def getFriends(userId: UUID): Task[List[UUID]] =
    friendshipStorage.getFriends(userId)

  override def getFollowers(userId: UUID): Task[List[UUID]] =
    friendshipStorage.getFollowers(userId)

  override def getAllFriends: Task[List[UUID]] =
    friendshipStorage.getAllFriends

  override def getAllFollowers: Task[List[UUID]] =
    friendshipStorage.getAllFollowers

object FriendshipServiceLive:
  val layer
      : URLayer[FriendshipStorage & RebuildCacheService, FriendshipService] =
    ZLayer.fromFunction(FriendshipServiceLive.apply _)
