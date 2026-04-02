package ru.socnetwork.service

import zio.Task

trait CsvImport:

  def importCsv(): Task[Unit]
