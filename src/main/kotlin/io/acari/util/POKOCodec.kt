package io.acari.util

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

const val INT_LENGTH = 4

open class POKOCodec<T>(private val serializer: (JsonObject, T) -> JsonObject,
                        private val deserializer: (JsonObject) -> T,
                        private val name: String) : MessageCodec<T, T> {
  override fun decodeFromWire(pos: Int, buffer: Buffer): T {
    val messageLength = buffer.getInt(pos)
    val startPosition = pos + INT_LENGTH
    val endPosition = startPosition + messageLength
    val jsonString = buffer.getString(startPosition, endPosition)
    val jsonObject = JsonObject(jsonString)
    return deserializer(jsonObject)
  }

  override fun systemCodecID(): Byte = -1

  override fun encodeToWire(buffer: Buffer, t: T) {
    val jsonToSend = JsonObject()
    val jsonMessage = serializer(jsonToSend, t).encode().toByteArray()
    val messageLength = jsonMessage.size
    buffer.appendInt(messageLength)
    buffer.appendBytes(jsonMessage)
  }

  override fun transform(t: T): T = t

  override fun name(): String = name

}
