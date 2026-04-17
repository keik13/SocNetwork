package ru.socnetwork.storage

import io.getquill.*
import ru.socnetwork.db.DbStrategy
import zio.{Task, URLayer, ZLayer}

import java.time.LocalDate
import java.util.UUID

final case class PostRow(
    id: UUID,
    userId: UUID,
    message: String,
    createdAt: Long,
    updatedAt: Long
)

final case class PostStorageLive(db: DbStrategy) extends PostStorage:

  import db.ctx.*

  private inline def queryPost = quote(
    querySchema[PostRow](entity = "post")
  )

  private inline def queryFriendship = quote(
    querySchema[FriendshipRow](entity = "friendship")
  )

  private inline def queryPostFriendship(userId: UUID) = quote(
    queryPost
      .join(queryFriendship)
      .on((p, f) => p.userId == f.friendId && f.userId == lift(userId))
  )

  override def add(postRow: PostRow): Task[Unit] =
    db.write(queryPost.insertValue(lift(postRow))).unit

  override def update(
      id: UUID,
      text: String,
      userId: UUID,
      updatedAt: Long
  ): Task[Unit] = db
    .write(
      queryPost
        .filter(_.id == lift(id))
        .filter(_.userId == lift(userId))
        .update(_.message -> lift(text), _.updatedAt -> lift(updatedAt))
    )
    .unit

  override def getById(id: UUID): Task[Option[PostRow]] =
    db.read(queryPost.filter(_.id == lift(id))).map(_.headOption)

  override def delete(id: UUID, userId: UUID): Task[Unit] = db
    .write(
      queryPost.filter(_.id == lift(id)).filter(_.userId == lift(userId)).delete
    )
    .unit

  override def getFriendPosts(count: Index, userId: UUID): Task[List[PostRow]] =
    db
      .read(
        queryPostFriendship(userId)
          .sortBy(_._1.updatedAt)(Ord.desc)
          .take(lift(count))
      )
      .map(_.map(_._1))

object PostStorageLive:
  val layer: URLayer[DbStrategy, PostStorage] =
    ZLayer.fromFunction(PostStorageLive.apply _)
