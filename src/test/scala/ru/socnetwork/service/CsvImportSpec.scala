package ru.socnetwork.service

import ru.socnetwork.auth.JwtServiceLive
import ru.socnetwork.conf.{Configuration, JwtConfig}
import ru.socnetwork.containers.{Containers, DbMigrationAspect}
import ru.socnetwork.db.{Db, DbStrategy}
import ru.socnetwork.storage.{UserStorage, UserStorageLive}
import zio.{ZEnvironment, ZIO}
import zio.test.*
import zio.test.TestAspect.sequential

object CsvImportSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("CsvImport")(
      test("should import users") {
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
      Configuration.layer.map(c => ZEnvironment(c.get[JwtConfig])),
      Containers.layer,
      Db.dataSourceLayer,
      Db.quillMasterLayer,
      Db.quillSlaveLayer,
      DbStrategy.layer,
      Containers.postgresLayer
    ) @@ sequential
