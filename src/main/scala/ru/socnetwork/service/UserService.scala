package ru.socnetwork.service

import ru.socnetwork.api.{
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  User,
  UserIdResponse
}
import zio.Task

import java.util.UUID

trait UserService:

  def login(login: LoginRequest): Task[Option[TokenResponse]]

  def register(register: RegisterRequest): Task[UserIdResponse]

  def getById(id: UUID): Task[Option[User]]

  def search(firstName: String, lastName: String): Task[List[User]]
