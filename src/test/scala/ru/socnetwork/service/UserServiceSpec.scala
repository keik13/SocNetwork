package ru.socnetwork.service

import ru.socnetwork.api.{LoginRequest, RegisterRequest, User}
import ru.socnetwork.conf.Configuration
import ru.socnetwork.containers.DbMigrationAspect
import ru.socnetwork.db.{Db, DbStrategy}
import ru.socnetwork.storage.{UserStorage, UserStorageLive}
import ru.socnetwork.containers.Containers
import zio.ZIO
import zio.test.*
import zio.test.TestAspect.sequential

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate}
import java.util.UUID

object UserServiceSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("UserService")(
      test("should registered user") {
        for userId <- ZIO.serviceWithZIO[UserService](_.register(user))
        yield assertTrue(
          userId.userId == UUID.fromString(
            "b2c8ccb8-191a-4233-9b34-3e3111a4adaf"
          )
        )
      },
      test("should get user") {
        for
          userService <- ZIO.service[UserService]
          userId <- userService.register(user)
          user <- userService.getById(userId.userId)
        yield assertTrue(
          user.contains(
            User(
              UUID.fromString("b2c8ccb8-191a-4233-9b34-3e3111a4adaf"),
              "Vasya",
              "Driszt",
              LocalDate.of(2000, 1, 1),
              "Very bored man",
              "Gvatemala"
            )
          )
        )
      },
      test("should login registered user") {
        for
          userService <- ZIO.service[UserService]
          userId <- userService.register(user)
          token <- userService.login(LoginRequest(userId.userId, user.password))
        yield assertTrue(token.nonEmpty)
      },
      test("should search registered user") {
        for
          userService <- ZIO.service[UserService]
          _ <- userService.register(user)
          _ <- userService.register(
            user.copy(firstName = "Vasyaaaa", secondName = "Drisztdfslgkjd")
          )
          _ <- userService.register(
            user.copy(firstName = "John", secondName = "Cow")
          )
          users <- userService.search("Vas", "Dri")
        yield assertTrue(users.size == 2)
      }
    ) @@ DbMigrationAspect.migrateOnce()() @@ TestAspect.after(
      ZIO.serviceWithZIO[UserStorage](_.deleteAll())
    )
  }
    .provideShared(
      UserServiceLive.layer,
      UserStorageLive.layer,
      PasswordServiceLive.layer,
      JwtServiceLive.layer,
      Configuration.layer,
      Containers.layer >>> Db.dataSourceLayer,
      Db.quillMasterLayer,
      Db.quillSlaveLayer,
      DbStrategy.layer,
      Containers.postgresLayer
    ) @@ sequential

  val user = RegisterRequest(
    "Vasya",
    "Driszt",
    LocalDate.of(2000, 1, 1),
    "Very bored man",
    "Gvatemala",
    "12345G"
  )
