package ru.socnetwork.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import ru.socnetwork.conf.DbConfig
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource

case class Master(quill: Quill.Postgres[SnakeCase])
case class Slave(quill: Quill.Postgres[SnakeCase])

object Db:
  def create(dbConfig: DbConfig): HikariDataSource =
    val poolConfig = new HikariConfig()
    poolConfig.setJdbcUrl(dbConfig.urlMaster)
    poolConfig.setUsername(dbConfig.user)
    poolConfig.setPassword(dbConfig.password)
    new HikariDataSource(poolConfig)

  def createSlave(dbConfig: DbConfig): HikariDataSource =
    val poolConfig = new HikariConfig()
    poolConfig.setJdbcUrl(dbConfig.urlSlave)
    poolConfig.setUsername(dbConfig.user)
    poolConfig.setPassword(dbConfig.password)
    new HikariDataSource(poolConfig)

  // Used for migration and executing create, update, delete queries.
  val dataSourceLayer: URLayer[DbConfig, DataSource] =
    ZLayer.scoped {
      ZIO.fromAutoCloseable {
        for
          dbConfig <- ZIO.service[DbConfig]
          dataSource <- ZIO.succeed(create(dbConfig))
        yield dataSource
      }
    }

  // Used for executing get queries.
  private val dataSourceSlaveLayer: URLayer[DbConfig, DataSource] =
    ZLayer.scoped {
      ZIO.fromAutoCloseable {
        for
          dbConfig <- ZIO.service[DbConfig]
          dataSource <- ZIO.succeed(createSlave(dbConfig))
        yield dataSource
      }
    }

  val quillMasterLayer: URLayer[DataSource, Master] =
    Quill.Postgres
      .fromNamingStrategy(SnakeCase)
      .map(env => ZEnvironment(Master(env.get[Quill.Postgres[SnakeCase]])))

  val quillSlaveLayer: URLayer[DbConfig, Slave] =
    dataSourceSlaveLayer >>> Quill.Postgres
      .fromNamingStrategy(SnakeCase)
      .map(env => ZEnvironment(Slave(env.get[Quill.Postgres[SnakeCase]])))
