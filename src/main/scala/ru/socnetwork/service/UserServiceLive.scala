package ru.socnetwork.service

import ru.socnetwork.api.{
  UserInfo,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  User,
  UserIdResponse
}
import ru.socnetwork.service.UserServiceLive.toUser
import ru.socnetwork.storage.{UserRow, UserStorage}
import zio.{Random, Task, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class UserServiceLive(
    userStorage: UserStorage,
    passwordService: PasswordServiceLive,
    jwtService: JwtService
) extends UserService:

  override def login(login: LoginRequest): Task[Option[TokenResponse]] = (for
    user <- userStorage.getById(login.id).some
    isValid <- passwordService
      .verifyPassword(login.password, user.password)
      .asSomeError
    token <- ZIO.when(isValid)(jwtService.generate(UserInfo(login.id))).some
  yield TokenResponse(token)).unsome

  override def register(register: RegisterRequest): Task[UserIdResponse] = for
    pass <- passwordService.hashPassword(register.password)
    uuid <- Random.nextUUID
    user <- userStorage.register(
      UserRow(
        uuid,
        register.firstName,
        register.secondName,
        register.birthdate,
        register.biography,
        register.city,
        pass
      )
    )
  yield UserIdResponse(uuid)

  override def getById(id: UUID): Task[Option[User]] =
    (for userRow <- userStorage.getById(id).some
    yield toUser(userRow)).unsome

  override def search(firstName: String, lastName: String): Task[List[User]] =
    userStorage.search(firstName, lastName).map(_.map(toUser))

object UserServiceLive:
  val layer: URLayer[
    UserStorage with PasswordServiceLive with JwtService,
    UserService
  ] =
    ZLayer.fromFunction(UserServiceLive.apply _)

  def toUser(row: UserRow) = User(
    row.id,
    row.firstName,
    row.secondName,
    row.birthdate,
    row.biography,
    row.city
  )
