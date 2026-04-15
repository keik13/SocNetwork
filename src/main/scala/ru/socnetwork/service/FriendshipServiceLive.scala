package ru.socnetwork.service

import ru.socnetwork.storage.FriendshipStorage
import zio.{Task, URLayer, ZLayer}

import java.util.UUID

final case class FriendshipServiceLive(
    friendshipStorage: FriendshipStorage
) extends FriendshipService:

  override def add(userId: UUID, friendUserId: UUID): Task[Unit] =
    friendshipStorage.add(userId, friendUserId)

  override def delete(userId: UUID, friendUserId: UUID): Task[Unit] =
    friendshipStorage.delete(userId, friendUserId)

  override def getFriends(userId: UUID): Task[List[UUID]] =
    friendshipStorage.getFriends(userId)

object FriendshipServiceLive:
  val layer: URLayer[FriendshipStorage, FriendshipService] =
    ZLayer.fromFunction(FriendshipServiceLive.apply _)
