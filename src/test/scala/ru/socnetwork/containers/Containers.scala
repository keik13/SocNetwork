package ru.socnetwork.containers

import zio.{ULayer, ZIO, ZLayer}
import org.testcontainers.containers.PostgreSQLContainer
import ru.socnetwork.conf.DbConfig

object Containers:

  val postgresLayer: ULayer[PostgreSQLContainer[Nothing]] = ZLayer.scoped {
    ZIO.acquireRelease(ZIO.attempt {
      val c = new PostgreSQLContainer("postgres:14-alpine")
      c.start()
      c
    }.orDie)(c => ZIO.attempt(c.stop()).ignoreLogged)
  }

  val layer: ZLayer[PostgreSQLContainer[Nothing], Throwable, DbConfig] =
    ZLayer.fromZIO {
      for
        container <- ZIO.service[PostgreSQLContainer[Nothing]]
        jdbcUrl = container.getJdbcUrl
        username = container.getUsername
        password = container.getPassword
      yield DbConfig(jdbcUrl, jdbcUrl, username, password)
    }
