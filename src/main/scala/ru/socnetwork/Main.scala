package ru.socnetwork

import ru.socnetwork.conf.Configuration
import ru.socnetwork.db.{Db, DbMigrator}
import ru.socnetwork.server.SocNetworkServer
import ru.socnetwork.service.{
  CsvImportLive,
  JwtServiceLive,
  PasswordServiceLive,
  UserServiceLive
}
import ru.socnetwork.storage.UserStorageLive
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object Main extends ZIOAppDefault:
  override val run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[SocNetworkServer](_.start)
      .provide(
        Configuration.layer,
        Db.dataSourceLayer,
        Db.quillLayer,
        DbMigrator.layer,
        SocNetworkServer.layer,
        UserServiceLive.layer,
        UserStorageLive.layer,
        PasswordServiceLive.layer,
        JwtServiceLive.layer,
        CsvImportLive.layer
      )
