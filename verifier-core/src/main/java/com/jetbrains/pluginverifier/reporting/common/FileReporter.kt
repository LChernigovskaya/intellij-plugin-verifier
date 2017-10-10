package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.reporting.Reporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class FileReporter<in T>(private val file: File,
                              private val lineProvider: (T) -> String = { it.toString() }) : Reporter<T> {

  private val fileWriter by lazy { openFileWriter() }

  private val lock: ReentrantLock = ReentrantLock(true)

  private var isClosed: Boolean = false

  private fun openFileWriter(): BufferedWriter? = try {
    file.create().bufferedWriter()
  } catch (e: Exception) {
    ERROR_LOGGER.error("Failed to open file writer for $file", e)
    null
  }

  override fun report(t: T) {
    val line = lineProvider(t)
    lock.withLock {
      if (!isClosed) {
        try {
          fileWriter?.appendln(line)
        } catch (e: Exception) {
          isClosed = true
          ERROR_LOGGER.error("Failed to report into $file", e)
        }
      }
    }
  }

  override fun close() {
    lock.withLock {
      if (!isClosed) {
        isClosed = true
        try {
          fileWriter?.close()
        } catch (e: Exception) {
          ERROR_LOGGER.error("Failed to close file writer for $file", e)
        }
      }
    }
  }

  private companion object {
    val ERROR_LOGGER: Logger = LoggerFactory.getLogger(FileReporter::class.java)
  }
}