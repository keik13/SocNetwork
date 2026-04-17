package ru.socnetwork.server

import ru.socnetwork.api.{
  ErrorResponse,
  LoginRequest,
  PostCreateRequest,
  PostResponse,
  PostUpdateRequest,
  RegisterRequest,
  TokenResponse,
  User,
  UserInfo
}
import ru.socnetwork.db.DbMigrator
import ru.socnetwork.server.SocNetworkServer.{
  fromOption,
  parseBody,
  searchParams
}
import ru.socnetwork.service.{
  CacheService,
  CsvImport,
  FriendshipService,
  PostService,
  RebuildCacheService,
  UserService
}
import ru.socnetwork.util.{InvalidBody, InvalidToken, MissingParams}
import zio.http.*
import zio.json.{EncoderOps, JsonDecoder, JsonEncoder}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class SocNetworkServer(
    userService: UserService,
    friendshipService: FriendshipService,
    postService: PostService,
    cacheService: CacheService,
    rebuildCacheService: RebuildCacheService,
    importCsv: CsvImport,
    migrator: DbMigrator,
    authMiddleware: AuthMiddleware
):

  private val userRoutes =
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
            .fromOption(
              searchParams(
                req.queryParam("first_name"),
                req.queryParam("last_name")
              )
            )
            .orElseFail(MissingParams)
          r <- userService.search(fullName._1, fullName._2)
        yield Response.json(r.toJson)
      },
      Method.POST / "user" / "import" -> handler { (req: Request) =>
        for _ <- importCsv.importCsv()
        yield Response.ok
      }
    )

  private val friendRoutes =
    Routes(
      Method.PUT / "friend" / "set" / uuid("userId") -> handler {
        (userId: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for r <- friendshipService.add(user.userId, userId)
            yield Response.ok
          }
      },
      Method.PUT / "friend" / "delete" / uuid("userId") -> handler {
        (userId: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for r <- friendshipService.delete(user.userId, userId)
            yield Response.ok
          }
      }
    )

  private val postRoutes =
    Routes(
      Method.POST / "post" / "create" -> handler { (req: Request) =>
        withContext { (user: UserInfo) =>
          for
            e <- parseBody[PostCreateRequest](req)
            r <- postService.add(e, user.userId)
          yield Response.json(r.toJson)
        }
      },
      Method.PUT / "post" / "update" -> handler { (req: Request) =>
        withContext { (user: UserInfo) =>
          for
            e <- parseBody[PostUpdateRequest](req)
            r <- postService.update(e, user.userId)
          yield Response.ok
        }
      },
      Method.PUT / "post" / "delete" / uuid("id") -> handler {
        (id: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for r <- postService.delete(id, user.userId)
            yield Response.ok
          }
      },
      Method.GET / "post" / "get" / uuid("id") -> handler {
        (id: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for r <- postService.getById(id)
            yield fromOption[PostResponse](r)
          }
      },
      Method.GET / "post" / "feed" -> handler { (req: Request) =>
        withContext { (user: UserInfo) =>
          for
            offsetLimit <- ZIO
              .fromOption(
                searchParams(
                  req.queryParam("offset"),
                  req.queryParam("limit")
                )
              )
              .map(ol => (ol._1.toInt, ol._2.toInt))
              .orElseSucceed((0, 10))
            r <- cacheService.feed(offsetLimit._1, offsetLimit._2, user.userId)
          yield Response.json(r.toJson)
        }
      }
    )

  private val adminRoutes =
    Routes(
      Method.POST / "post" / "feed" / "rebuild" -> handler { (req: Request) =>
        for r <- rebuildCacheService.rebuildAllFollowerCache()
        yield Response.ok
      }
    )

  private val app =
    (adminRoutes ++ userRoutes ++ (postRoutes ++ friendRoutes) @@ authMiddleware.jwtAuthentication)
      .handleErrorZIO {
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
  val layer: URLayer[
    UserService
      with DbMigrator
      with CsvImport
      with FriendshipService
      with PostService
      with CacheService
      with RebuildCacheService
      with AuthMiddleware,
    SocNetworkServer
  ] =
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

  def searchParams(
      firstParam: Option[String],
      secondParam: Option[String]
  ): Option[(String, String)] =
    (firstParam, secondParam) match
      case (Some(fn), Some(ln)) => Some((fn, ln))
      case _                    => None
