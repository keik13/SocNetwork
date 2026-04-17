package ru.socnetwork.service

import zio.Task

import java.util.UUID

trait FriendshipService:

  def add(userId: UUID, friendUserId: UUID): Task[Unit]

  def delete(userId: UUID, friendUserId: UUID): Task[Unit]

  def getFriends(userId: UUID): Task[List[UUID]]

  def getAllFriends: Task[List[UUID]]

  def getFollowers(userId: UUID): Task[List[UUID]]

  def getAllFollowers: Task[List[UUID]]
