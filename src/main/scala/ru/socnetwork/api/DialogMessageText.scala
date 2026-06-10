package ru.socnetwork.api

import zio.json.*

import java.util.UUID

@jsonMemberNames(SnakeCase)
final case class DialogMessageText(text: String) derives JsonCodec

@jsonMemberNames(SnakeCase)
final case class DialogMessage(from: UUID, to: UUID, text: String)
    derives JsonCodec
