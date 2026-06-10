package ru.socnetwork.service

import ru.socnetwork.api.{DialogMessage, DialogMessageText}
import zio.Task
import zio.http.Response

import java.util.UUID

trait DialogMessageService:

  def add(request: DialogMessageText, toUserId: UUID): Task[Response]

  def getById(toUserId: UUID): Task[Response]
