package ru.socnetwork.service

import ru.socnetwork.conf.Configuration
import ru.socnetwork.containers.{Containers, DbMigrationAspect}
import ru.socnetwork.db.Db
import ru.socnetwork.storage.{UserStorage, UserStorageLive}
import zio.ZIO
import zio.test.*
import zio.test.TestAspect.sequential

object CsvImportSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("UserService")(
      test("should registered user") {
        for
          importCsv <- ZIO.serviceWithZIO[CsvImport](_.importCsv())
          users <- ZIO.serviceWithZIO[UserService](_.search("", "Абрамов"))
        yield assertTrue(users.size == 4)
      }
    ) @@ DbMigrationAspect.migrateOnce()() @@ TestAspect.after(
      ZIO.serviceWithZIO[UserStorage](_.deleteAll())
    )
  }
    .provideShared(
      CsvImportLive.layer,
      UserServiceLive.layer,
      UserStorageLive.layer,
      PasswordServiceLive.layer,
      JwtServiceLive.layer,
      Configuration.layer,
      Containers.layer >>> Db.dataSourceLayer,
      Db.quillLayer,
      Containers.postgresLayer
    ) @@ sequential
