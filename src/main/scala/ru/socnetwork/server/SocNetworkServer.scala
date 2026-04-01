package ru.socnetwork.server

import ru.socnetwork.api.{
  ErrorResponse,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  User
}
import ru.socnetwork.db.DbMigrator
import ru.socnetwork.server.SocNetworkServer.{
  fromOption,
  parseBody,
  searchParams
}
import ru.socnetwork.service.UserService
import ru.socnetwork.util.{InvalidBody, InvalidToken, MissingParams}
import zio.http.*
import zio.json.{EncoderOps, JsonDecoder, JsonEncoder}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class SocNetworkServer(
    userService: UserService,
    migrator: DbMigrator
):

  private val antifraudCustomRoutes =
    Routes(
      Method.POST / "login" -> handler { (req: Request) =>
        for
          e <- parseBody[LoginRequest](req)
          r <- userService.login(e)
        yield fromOption[TokenResponse](r)
      },
      Method.POST / "user" / "register" -> handler { (req: Request) =>
        for
          e <- parseBody[RegisterRequest](req)
          r <- userService.register(e)
        yield Response.json(r.toJson)
      },
      Method.GET / "user" / "get" / uuid("id") -> handler {
        (id: UUID, req: Request) =>
          for r <- userService.getById(id)
          yield fromOption[User](r)
      },
      Method.GET / "user" / "search" -> handler { (req: Request) =>
        for
          fullName <- ZIO
            .fromOption(searchParams(req))
            .orElseFail(MissingParams)
          r <- userService.search(fullName._1, fullName._2)
        yield Response.json(r.toJson)
      }
    ).handleErrorZIO {
      case InvalidBody | InvalidToken | MissingParams =>
        ZIO.succeed(Response.badRequest)
      case err: Throwable =>
        ZIO
          .logError(err.getMessage)
          .as(
            Response
              .json(ErrorResponse(err.getMessage, "", 0).toJson)
              .status(Status.InternalServerError)
          )
    }

  private val app: Routes[Any, Nothing] = antifraudCustomRoutes

  private def run: ZIO[Any, Throwable, Nothing] = Server
    .serve(app)
    .provide(Server.default)
    .tapError(err => ZIO.logError(err.getMessage))

  def start: ZIO[Any, Throwable, Unit] =
    for
      _ <- migrator.migrate
      _ <- run
    yield ()

object SocNetworkServer:
  val layer: URLayer[UserService with DbMigrator, SocNetworkServer] =
    ZLayer.fromFunction(SocNetworkServer.apply _)

  def parseBody[A: JsonDecoder](request: Request): IO[InvalidBody.type, A] =
    request.body
      .asJsonFromCodec[A]
      .tapError(err => ZIO.logError(err.getMessage))
      .orElseFail(InvalidBody)

  def fromOption[A: JsonEncoder](opt: Option[A]): Response =
    opt match
      case Some(value) => Response.json(value.toJson)
      case None        => Response.notFound

  def searchParams(req: Request): Option[(String, String)] =
    (req.queryParam("first_name"), req.queryParam("last_name")) match
      case (Some(fn), Some(ln)) => Some((fn, ln))
      case _                    => None
