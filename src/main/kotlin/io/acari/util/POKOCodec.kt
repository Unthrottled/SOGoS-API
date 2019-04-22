package io.acari.util

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

open class POKOCodec<T>(private val seralizer: (JsonObject) -> Unit) : MessageCodec<T, T> {
  override fun decodeFromWire(pos: Int, buffer: Buffer?): T {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun systemCodecID(): Byte = -1

  override fun encodeToWire(buffer: Buffer, s: T) {
    val jsonToSend = JsonObject()
    seralizer(jsonToSend)
    val jsonMessage = jsonToSend.encode().toByteArray()
    val messageLength = jsonMessage.size
    buffer.appendInt(messageLength)
    buffer.appendBytes(jsonMessage)
  }

  override fun transform(s: T): T {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun name(): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}
