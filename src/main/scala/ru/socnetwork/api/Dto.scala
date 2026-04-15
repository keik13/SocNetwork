package ru.socnetwork.api

import zio.json.*
import zio.schema.{Schema, derived}

import java.time.LocalDate
import java.util.UUID

@jsonMemberNames(SnakeCase)
final case class LoginRequest(id: UUID, password: String) derives JsonDecoder

@jsonMemberNames(SnakeCase)
final case class RegisterRequest(
    firstName: String,
    secondName: String,
    birthdate: LocalDate,
    biography: String,
    city: String,
    password: String
) derives JsonDecoder

@jsonMemberNames(SnakeCase)
final case class TokenResponse(token: String) derives JsonEncoder

@jsonMemberNames(SnakeCase)
final case class UserIdResponse(userId: UUID) derives JsonEncoder

@jsonMemberNames(SnakeCase)
final case class User(
    id: UUID,
    firstName: String,
    secondName: String,
    birthdate: LocalDate,
    biography: String,
    city: String
) derives JsonEncoder

@jsonMemberNames(SnakeCase)
final case class ErrorResponse(message: String, requestId: String, code: Int)
    derives JsonEncoder

final case class UserInfo(userId: UUID) derives JsonCodec

@jsonMemberNames(SnakeCase)
final case class PostCreateRequest(text: String) derives JsonDecoder

@jsonMemberNames(SnakeCase)
final case class PostUpdateRequest(id: UUID, text: String) derives JsonDecoder

@jsonMemberNames(SnakeCase)
final case class PostResponse(id: UUID, text: String, authorUserId: UUID)
    derives JsonEncoder,
      Schema
