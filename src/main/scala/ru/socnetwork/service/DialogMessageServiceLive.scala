package ru.socnetwork.service

import ru.socnetwork.api.{DialogMessageText, ErrorResponse}
import ru.socnetwork.conf.DialogConfig
import ru.socnetwork.server.RequestIdMiddleware.requestId
import zio.http.{Body, Client, Header, Request, Response, Status, ZClientAspect}
import zio.json.EncoderOps
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class DialogMessageServiceLive(
    baseClient: Client,
    dialogConfig: DialogConfig
) extends DialogMessageService:

  private val client = (baseClient @@ ZClientAspect.forwardHeaders).batched

  override def add(request: DialogMessageText, toUserId: UUID): Task[Response] =
    for
      requestId <- requestId
      r <- client(
        Request
          .post(
            s"${dialogConfig.url}/dialog/$toUserId/send",
            Body.fromString(request.toJson)
          )
          .addHeader(Header.Custom("X-Request-ID", requestId))
      )
      _ <-
        if r.status.isSuccess then
          ZIO.logTrace(
            s"SocNetwork server received ok answer on send to dialog $requestId"
          )
        else
          ZIO.fail(
            ErrorResponse(
              s"SocNetwork server response has been received with error. Response status ${r.status}",
              requestId,
              r.status.code
            )
          )
    yield r

  override def getById(toUserId: UUID): Task[Response] =
    for
      requestId <- requestId
      r <- client(
        Request
          .get(s"${dialogConfig.url}/dialog/$toUserId/list")
          .addHeader(Header.Custom("X-Request-ID", requestId))
      )
      _ <-
        if r.status == Status.Ok then
          ZIO.logTrace(
            s"SocNetwork server received ok answer on list from dialog $requestId"
          )
        else
          ZIO.fail(
            ErrorResponse(
              s"Response has been received with error. Response status ${r.status}",
              requestId,
              r.status.code
            )
          )
    yield r

object DialogMessageServiceLive:
  val layer: URLayer[Client & DialogConfig, DialogMessageService] =
    ZLayer.fromFunction(DialogMessageServiceLive.apply _)
