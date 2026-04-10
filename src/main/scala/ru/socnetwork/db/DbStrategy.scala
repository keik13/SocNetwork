package ru.socnetwork.db

import io.getquill.jdbczio.Quill
import io.getquill.{Action, Query, Quoted, SnakeCase}
import ru.socnetwork.db.{Master, Slave}
import zio.{Task, URLayer, ZIO, ZLayer}

case class DbStrategy(master: Master, slave: Slave):
  // 1. Импортируем общие типы (Query, Action и т.д.)
  // Используем мастер-контекст как "эталон" для типов (они идентичны)
  val ctx: Quill.Postgres[SnakeCase] = master.quill

  // 2. Метод для чтения (идет на Slave)
  inline def read[T](quoted: Quoted[Query[T]]): Task[List[T]] =
    slave.quill.run(quoted)

  // 3. Метод для записи (идет на Master)
  inline def write[T](quoted: Quoted[Action[T]]): Task[Long] =
    master.quill.run(quoted)

  // Можно добавить метод для транзакций (всегда на Master)
  inline def transaction[R, A](
      zio: ZIO[R, Throwable, A]
  ): ZIO[R, Throwable, A] =
    master.quill.transaction(zio)

object DbStrategy:
  val layer: URLayer[Master & Slave, DbStrategy] = ZLayer.derive[DbStrategy]
