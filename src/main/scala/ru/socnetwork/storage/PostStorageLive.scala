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

object PostStorageLive:
  val layer: URLayer[DbStrategy, PostStorage] =
    ZLayer.fromFunction(PostStorageLive.apply _)
