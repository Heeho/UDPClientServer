package common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecoderTest {
    private val decoder = Decoder(byteArrayOf(1, 0,0,0,1, 0,0,0,0,0,0,0,1))

    @BeforeEach
    fun init() = decoder.reset()

    @Test
    fun read() {
        assertEquals(decoder.readByte(), decoder.buffer[0])
        assertEquals(decoder.readInt(), decoder.buffer[4].toInt())
        assertEquals(decoder.readLong(), decoder.buffer[12].toLong())
    }

    @Test
    fun reset() {
        decoder.readByte()
        decoder.readByte()
        decoder.reset()
        val b = decoder.readByte()
        assertEquals(b, decoder.buffer.first())
    }
}