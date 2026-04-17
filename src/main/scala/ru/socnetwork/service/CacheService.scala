package ru.socnetwork.service

import ru.socnetwork.api.PostResponse
import zio.{Chunk, Task}

import java.util.UUID

trait CacheService:

  def updateCache(
      text: String,
      userId: UUID,
      uuid: UUID,
      followerIds: List[UUID]
  ): Task[Unit]

  def rebuildCache(userId: UUID, postResp: List[PostResponse]): Task[Unit]

  def feed(offset: Int, limit: Int, userId: UUID): Task[Chunk[PostResponse]]
