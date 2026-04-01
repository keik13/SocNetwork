package ru.socnetwork.storage

import io.getquill.*
import io.getquill.jdbczio.Quill
import ru.socnetwork.api.{
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  User,
  UserIdResponse
}
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

final case class UserStorageLive(quill: Quill.Postgres[SnakeCase])
    extends UserStorage:

  import quill.*

  private inline def queryUser = quote(
    querySchema[UserRow](entity = "soc_user")
  )

  override def register(user: UserRow): Task[Unit] = run(
    queryUser.insertValue(lift(user))
  ).unit

  override def getById(id: UUID): Task[Option[UserRow]] = run(
    queryUser.filter(_.id == lift(id))
  ).map(_.headOption)

  override def search(
      firstName: String,
      lastName: String
  ): Task[List[UserRow]] = run(
    queryUser
      .filter(_.firstName like lift(s"$firstName%"))
      .filter(_.secondName like lift(s"$lastName%"))
  )

  override def deleteAll(): Task[Unit] = run(queryUser.delete).unit

object UserStorageLive:
  val layer: URLayer[Quill.Postgres[SnakeCase], UserStorage] =
    ZLayer.fromFunction(UserStorageLive.apply _)
