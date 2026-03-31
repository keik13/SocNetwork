package ru.socnetwork.api

import zio.json.*

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

final case class JwtClaims(userId: UUID) derives JsonCodec
