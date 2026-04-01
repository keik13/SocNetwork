package ru.socnetwork.storage

import ru.socnetwork.api.{
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  User,
  UserIdResponse
}
import zio.Task

import java.util.UUID

trait UserStorage:

  def register(user: UserRow): Task[Unit]

  def getById(id: UUID): Task[Option[UserRow]]

  def deleteAll(): Task[Unit]

  def search(firstName: String, lastName: String): Task[List[UserRow]]
