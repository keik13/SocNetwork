package ru.socnetwork.service

import zio.UIO
import zio.http.WebSocketChannel

import java.util.UUID

type ConnectionMap = Map[UUID, Set[WebSocketChannel]]

trait ConnectionService:
  def add(userId: UUID, channel: WebSocketChannel): UIO[Unit]
  def remove(userId: UUID, channel: WebSocketChannel): UIO[Unit]
  def getChannels(userId: UUID): UIO[Set[WebSocketChannel]]
  def getAllConnections: UIO[ConnectionMap]
