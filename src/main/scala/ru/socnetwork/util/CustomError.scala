package ru.socnetwork.util

trait CustomError extends Throwable

case object InvalidBody extends CustomError
