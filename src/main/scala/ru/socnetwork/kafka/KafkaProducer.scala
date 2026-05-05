package ru.socnetwork.kafka

import ru.socnetwork.api.PostResponse
import zio.Task

trait KafkaProducer:

  def produce(pr: PostResponse): Task[Unit]
