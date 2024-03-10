package common

import java.io.ByteArrayInputStream
import java.io.DataInputStream

class Decoder(
    val buffer: ByteArray
): Disposable  {
    private val bais = ByteArrayInputStream(buffer)
    private val dis = DataInputStream(bais)

    override fun dispose() = dis.close()

    fun readInt(): Int = dis.readInt()
    fun readByte(): Byte = dis.readByte()
    fun readLong(): Long = dis.readLong()

    fun reset() = dis.reset()
}
