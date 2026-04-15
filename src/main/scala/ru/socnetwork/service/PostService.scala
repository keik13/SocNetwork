package ru.socnetwork.service

import ru.socnetwork.api.{PostCreateRequest, PostResponse, PostUpdateRequest}
import zio.{Chunk, Task}

import java.util.UUID

trait PostService:

  def add(request: PostCreateRequest, userId: UUID): Task[UUID]

  def update(update: PostUpdateRequest, userId: UUID): Task[Unit]

  def getById(id: UUID): Task[Option[PostResponse]]

  def delete(id: UUID, userId: UUID): Task[Unit]

  def feed(offset: Int, limit: Int, userId: UUID): Task[Chunk[PostResponse]]
