package ru.socnetwork.service

import ru.socnetwork.api.RegisterRequest
import zio.{Random, Task, URLayer, ZIO, ZLayer}
import zio.stream.ZStream

import java.nio.file.Files
import java.time.LocalDate

final case class CsvImportLive(userService: UserService) extends CsvImport:

  override def importCsv(): Task[Unit] =
    ZStream
      .scoped(
        ZIO.fromAutoCloseable(
          ZIO.attemptBlocking(
            Files.newBufferedReader(java.nio.file.Paths.get("people.v2.csv"))
          )
        )
      )
      .flatMap(reader => ZStream.from(reader.lines()))
      .foreach(s =>
        for
          arr <- ZIO.attempt(s.split(","))
          bio <- Random.nextString(64)
          pass <- Random.nextString(12)
          _ <- userService.register(
            RegisterRequest(
              firstName = arr(0).split(" ")(1),
              secondName = arr(0).split(" ")(0),
              birthdate = LocalDate.parse(arr(1)),
              biography = bio,
              city = arr(2),
              password = pass
            )
          )
        yield ()
      )

object CsvImportLive:
  val layer: URLayer[UserService, CsvImport] =
    ZLayer.fromFunction(CsvImportLive.apply _)
