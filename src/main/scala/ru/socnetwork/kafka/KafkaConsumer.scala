package ru.socnetwork.kafka

import zio.stream.ZStream

trait KafkaConsumer:

  def consume: ZStream[Any, Throwable, Unit]
