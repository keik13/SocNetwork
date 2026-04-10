package ru.socnetwork.storage

import io.getquill.*
import ru.socnetwork.db.DbStrategy
import zio.{Task, URLayer, ZLayer}

import java.time.LocalDate
import java.util.UUID

final case class UserRow(
    id: UUID,
    firstName: String,
    secondName: String,
    birthdate: LocalDate,
    biography: String,
    city: String,
    password: String
)

final case class UserStorageLive(db: DbStrategy) extends UserStorage:

  import db.ctx.*

  private inline def queryUser = quote(
    querySchema[UserRow](entity = "soc_user")
  )

  override def register(user: UserRow): Task[Unit] =
    db.write(
      queryUser.insertValue(lift(user))
    ).unit

  override def getById(id: UUID): Task[Option[UserRow]] =
    db.read(
      queryUser.filter(_.id == lift(id))
    ).map(_.headOption)

  override def search(
      firstName: String,
      lastName: String
  ): Task[List[UserRow]] =
    db.read(
      queryUser
        .filter(_.firstName like lift(s"$firstName%"))
        .filter(_.secondName like lift(s"$lastName%"))
        .sortBy(_.id)
    )

  override def deleteAll(): Task[Unit] =
    db.write(queryUser.delete).unit

  override def getAll: Task[List[UserRow]] =
    db.read(queryUser)

object UserStorageLive:
  val layer: URLayer[DbStrategy, UserStorage] =
    ZLayer.fromFunction(UserStorageLive.apply _)
