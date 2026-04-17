package ru.socnetwork.storage

import zio.Task

import java.util.UUID

trait PostStorage:

  def add(postRow: PostRow): Task[Unit]

  def update(id: UUID, text: String, userId: UUID, updatedAt: Long): Task[Unit]

  def getById(id: UUID): Task[Option[PostRow]]

  def delete(id: UUID, userId: UUID): Task[Unit]

  def getFriendPosts(count: Int, userId: UUID): Task[List[PostRow]]
