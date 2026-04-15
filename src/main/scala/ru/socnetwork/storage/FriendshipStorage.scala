package ru.socnetwork.storage

import zio.Task

import java.util.UUID

trait FriendshipStorage:

  def add(userId: UUID, friendId: UUID): Task[Unit]

  def delete(userId: UUID, friendId: UUID): Task[Unit]

  def getFriends(userId: UUID): Task[List[UUID]]

  def getFollowers(userId: UUID): Task[List[UUID]]
