package ru.socnetwork.service

import ru.socnetwork.api.PostResponse
import zio.{Chunk, Task}

import java.util.UUID

trait RebuildCacheService:

  def rebuildAllFollowerCache(): Task[Unit]

  def rebuildFollowerCache(userId: UUID): Task[Unit]

  def rebuildFriendCache(userId: UUID): Task[Unit]
