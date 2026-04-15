package ru.socnetwork.storage

import io.getquill.*
import ru.socnetwork.db.DbStrategy
import zio.{Task, URLayer, ZLayer}

import java.time.LocalDate
import java.util.UUID

final case class FriendshipRow(userId: UUID, friendId: UUID)

final case class FriendshipStorageLive(db: DbStrategy)
    extends FriendshipStorage:

  import db.ctx.*

  private inline def queryFriendship = quote(
    querySchema[FriendshipRow](entity = "friendship")
  )

  override def add(userId: UUID, friendId: UUID): Task[Unit] = db
    .write(
      queryFriendship
        .insert(_.userId -> lift(userId), _.friendId -> lift(friendId))
    )
    .unit

  override def delete(userId: UUID, friendId: UUID): Task[Unit] =
    db.write(
      queryFriendship
        .filter(_.userId == lift(userId))
        .filter(_.friendId == lift(friendId))
        .delete
    ).unit

  def getFriends(userId: UUID): Task[List[UUID]] =
    db.read(queryFriendship.filter(_.userId == lift(userId)).map(_.friendId))

  override def getFollowers(userId: UUID): Task[List[UUID]] =
    db.read(queryFriendship.filter(_.friendId == lift(userId)).map(_.userId))

object FriendshipStorageLive:
  val layer: URLayer[DbStrategy, FriendshipStorage] =
    ZLayer.fromFunction(FriendshipStorageLive.apply _)
