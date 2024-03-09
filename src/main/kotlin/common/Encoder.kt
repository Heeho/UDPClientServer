package common

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class Encoder {
    private val baos = ByteArrayOutputStream()
    private val dos = DataOutputStream(baos)

    fun reset(): Encoder {
        baos.reset()
        return this
    }

    fun write(v: Byte): Encoder {
        dos.writeByte(v.toInt())
        return this
    }

    fun write(v: Int): Encoder {
        dos.writeInt(v)
        return this
    }

    fun write(v: Long): Encoder {
        dos.writeLong(v)
        return this
    }

    fun bytes(): ByteArray {
        dos.flush()
        val data = baos.toByteArray()
        baos.reset()
        return data
    }
}