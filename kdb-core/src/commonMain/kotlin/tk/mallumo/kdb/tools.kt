package tk.mallumo.kdb

import java.nio.ByteBuffer
import java.nio.ByteOrder

// 1. Convert FloatArray to little-endian ByteArray (for INSERT)
fun FloatArray.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
    buffer.asFloatBuffer().put(this)
    return buffer.array()
}

// 2. Convert little-endian ByteArray back to FloatArray (after SELECT)
fun ByteArray.toFloatArray(): FloatArray {
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    val floatBuffer = buffer.asFloatBuffer()
    val floats = FloatArray(floatBuffer.remaining())
    floatBuffer.get(floats)
    return floats
}