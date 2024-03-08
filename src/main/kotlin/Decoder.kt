import java.io.ByteArrayInputStream
import java.io.DataInputStream

class Decoder(
    private var inputbuffer: ByteArray = byteArrayOf(0)
) {
    private val bais = ByteArrayInputStream(inputbuffer)
    private val dis = DataInputStream(bais)

    init {
    }

    val inputbuffersize get() = inputbuffer.size

    fun bind(b: ByteArray) {
        inputbuffer = b
        dis.reset()
    }

    fun readInt(): Int = dis.readInt()
    fun readByte(): Byte = dis.readByte()
    fun readLong(): Long = dis.readLong()

    fun reset() {
        dis.reset()
    }
}