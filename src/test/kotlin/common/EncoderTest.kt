package common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EncoderTest {
    private val encoder = Encoder()
    private val testint = 42
    private val testbyte = testint.toByte()
    private val testlong = testint.toLong()
    private val testbytearray = byteArrayOf(11,22)

    @BeforeEach
    fun rewind() {
        encoder.reset()
    }

    @Test
    fun reset() {
        testbytearray.forEach { encoder.write(it) }
        encoder.reset()
        val b = encoder.write(testbyte).bytes()
        encoder.reset()
        assertEquals(b[0],testbyte)
    }

    @Test
    fun write() {
        val b = encoder.write(testbyte).write(testint).write(testbyte).write(testlong).write(testbyte).bytes()
        assertEquals(b[0],testbyte)
        assertEquals(b[0+4 + 1],testbyte)
        assertEquals(b[0+4+1+8 + 1],testbyte)
    }

    @Test
    fun bytes() {
        val b = encoder.write(testbyte).bytes()
        assertEquals(b[0],testbyte)
    }
}