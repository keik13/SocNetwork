//package ru.socnetwork.service
//
//import ru.socnetwork.api.{LoginRequest, RegisterRequest}
//import ru.socnetwork.auth.JwtServiceLive
//import ru.socnetwork.conf.{Configuration, DialogConfig, JwtConfig}
//import ru.socnetwork.containers.{Containers, DbMigrationAspect}
//import ru.socnetwork.db.{Db, DbMigrator, DbStrategy}
//import ru.socnetwork.mock.{KafkaConsumerTest, KafkaProducerTest}
//import ru.socnetwork.server.{AuthMiddleware, SocNetworkServer}
//import ru.socnetwork.service.RebuildCacheServiceSpec.ProtobufCodecSupplier
//import ru.socnetwork.storage.{
//  FriendshipStorageLive,
//  PostStorageLive,
//  UserStorage,
//  UserStorageLive
//}
//import zio.http.*
//import zio.redis.{CodecSupplier, Redis}
//import zio.test.*
//import zio.test.TestAspect.sequential
//import zio.{Random, Scope, ZEnvironment, ZIO, ZLayer, durationInt}
//
//import java.time.LocalDate
//
//object DialogServiceSpec extends ZIOSpecDefault:
//
//  override def spec: Spec[TestEnvironment with Scope, Throwable] = {
//    val body =
//      "{\"text\": \"Lectus mauris ultrices eros in cursus turpis massa. In fermentum et sollicitudin ac orci.\"}"
//    suite("DialogService")(
//      test("should send message") {
//        for
//          port <- ZIO.serviceWithZIO[Server](_.port)
//          _ <- TestServer.addRoutes {
//            Routes(
//              Method.POST / "dialog" / uuid("userId") / "send" -> handler {
//                (req: Request) =>
//                  // Проверяем, дошел ли заголовок до "внешнего" сервиса
//                  val hasXRequestID = req.hasHeader("X-Request-ID")
//                  val hasBearer = req.hasHeader(Header.Authorization.Bearer)
//                  if hasXRequestID && hasBearer then Response.ok
//                  else Response.status(Status.BadRequest)
//              }
//            )
//          }
//          userId <- ZIO.serviceWithZIO[UserService](_.register(user))
//          token <- ZIO.serviceWithZIO[UserService](
//            _.login(LoginRequest(userId.userId, user.password))
//          )
//          uuid <- Random.nextUUID
////          _ <- TestClient.addRequestResponse(
////            Request
////              .post(
////                "http://localhost:8081/dialog/550e8400-e29b-41d4-a716-446655440000/send",
////                Body.fromString(body)
////              )
////              .addHeader(Header.Authorization.Bearer(token.get.token))
////              .addHeader(Header.Custom("X-Request-ID", s"$uuid")),
////            Response.ok
////          )
//          server <- ZIO.service[SocNetworkServer]
//          routes = server.app.toHandler
//          rFromSend <- routes(
//            Request
//              .post(
//                "dialog/550e8400-e29b-41d4-a716-446655440000/send",
//                Body.fromString(body)
//              )
//              .addHeader(Header.Authorization.Bearer(token.get.token))
//              .addHeader(Header.Custom("X-Request-ID", s"$uuid"))
//          )
//        yield assertTrue(rFromSend.status.isSuccess)
//      }
//    ) @@ DbMigrationAspect.migrateOnce()() @@ TestAspect.after(
//      ZIO.serviceWithZIO[UserStorage](_.deleteAll())
//    ) @@ TestAspect.withLiveClock
//  }
//    .provideSomeShared[Scope](
//      UserServiceLive.layer,
//      UserStorageLive.layer,
//      PasswordServiceLive.layer,
//      JwtServiceLive.layer,
//      Configuration.layer.map(c =>
//        ZEnvironment(c.get[JwtConfig]).add(c.get[DialogConfig])
//      ),
//      Containers.layer,
//      Db.dataSourceLayer,
//      Db.quillMasterLayer,
//      Db.quillSlaveLayer,
//      DbStrategy.layer,
//      Containers.postgresLayer,
//      TestClient.layer,
//      SocNetworkServer.layer,
//      CsvImportLive.layer,
//      FriendshipServiceLive.layer,
//      FriendshipStorageLive.layer,
//      PostServiceLive.layer,
//      PostStorageLive.layer,
//      CacheServiceLive.layer,
//      RebuildCacheServiceLive.layer,
//      AuthMiddleware.layer,
//      Containers.redisLayer,
//      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
//      Redis.singleNode,
//      KafkaProducerTest.layer,
//      KafkaConsumerTest.layer,
//      DialogMessageServiceLive.layer,
//      ConnectionServiceLive.layer,
//      DbMigrator.layer,
//      TestServer.default,
//      ZLayer.succeed(Server.Config.default.port(8081))
//    ) @@ sequential
//
//  val user = RegisterRequest(
//    "Vasya",
//    "Driszt",
//    LocalDate.of(2000, 1, 1),
//    "Very bored man",
//    "Gvatemala",
//    "12345G"
//  )
