package ru.socnetwork.containers

import com.redis.testcontainers.RedisContainer
import zio.{Duration, ULayer, ZIO, ZLayer}
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import ru.socnetwork.conf.DbConfig
import zio.redis.{Redis, RedisConfig}

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

  val redisLayer: ZLayer[Any, Throwable, RedisConfig] = ZLayer.scoped {
    for
      container <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val c =
            new RedisContainer(DockerImageName.parse("redis:8-alpine"))
          c.withStartupTimeout(Duration.fromSeconds(120)).start()
          c
        }
      )(c => ZIO.attemptBlocking(c.stop()).orDie)

      host = container.getHost
      port = container.getFirstMappedPort
    yield RedisConfig(host, port)
  }
