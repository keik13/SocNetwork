package ru.socnetwork.service

import zio.{Ref, UIO, ULayer, ZLayer}
import zio.http.WebSocketChannel

import java.util.UUID

case class ConnectionServiceLive(ref: Ref[ConnectionMap])
    extends ConnectionService:
  override def add(userId: UUID, channel: WebSocketChannel): UIO[Unit] =
    ref
      .update(map =>
        map.updated(userId, map.getOrElse(userId, Set.empty) + channel)
      )
      .unit

  override def remove(userId: UUID, channel: WebSocketChannel): UIO[Unit] =
    ref.update { map =>
      map.get(userId) match
        case Some(channels) =>
          val updatedChannels = channels - channel
          if updatedChannels.isEmpty then map - userId
          else map.updated(userId, updatedChannels)
        case None => map
    }.unit

  override def getChannels(userId: UUID): UIO[Set[WebSocketChannel]] =
    ref.get.map(_.getOrElse(userId, Set.empty))

  override def getAllConnections: UIO[ConnectionMap] = ref.get

object ConnectionServiceLive:
  val layer: ULayer[ConnectionService] = ZLayer {
    Ref
      .make(Map.empty[UUID, Set[WebSocketChannel]])
      .map(ConnectionServiceLive(_))
  }
