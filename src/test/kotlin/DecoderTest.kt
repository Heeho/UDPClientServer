import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecoderTest {
    private val testint = 42
    private val testbyte = testint.toByte()
    private val testlong = testint.toLong()
    private val testbytearray = byteArrayOf(11,22)
    private val testbytearray2 = byteArrayOf(33,44,55)

    private val decoder = Decoder(testbytearray)

    @BeforeEach
    fun init() {
        decoder.reset()
    }

    @Test
    fun bind() {
        assertEquals(decoder.inputbuffersize, testbytearray.size)
        decoder.bind(testbytearray2)
        assertEquals(decoder.inputbuffersize, testbytearray2.size)
    }

    @Test
    fun read() {
        assertEquals(decoder.readByte(), testbytearray[0])
    }

    @Test
    fun reset() {
        decoder.readByte()
        decoder.readByte()
        decoder.reset()
        val b = decoder.readByte()
        assertEquals(b, testbytearray[0])
    }
}