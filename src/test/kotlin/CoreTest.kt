import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoreTest {
    @Test
    fun arrays() {
        val a = arrayOf(1,2,3,4)
        print(a.indexOf(3))
    }
    @Test
    fun threads() {
        var i = 0
        val limit = 5
        var working = true

        val thread = thread(start = false) {
            while(working) {
                if(++i == limit) working = false
            }
        }
        thread.start()
        sleep(1000)
        assertEquals(i,limit)
        assert(!thread.isAlive)
    }

    @Test
    fun sockets() {
        val port = 40708
        val socket = DatagramSocket(port)
        val inpacket = DatagramPacket(ByteArray(1), 1)
        val outpacket = DatagramPacket(ByteArray(1), 1)
        outpacket.port = port
        socket.soTimeout = 1000

        var i: Byte = 0
        var threadstopped = false

        thread {
            while(!threadstopped) {
                try {
                    socket.receive(inpacket)
                } catch(e: Exception) {
                    continue
                }
                assertEquals(inpacket.data[0],i)
            }
        }

        listOf(
            InetAddress.getLoopbackAddress(),
            InetAddress.getLocalHost(),
            InetAddress.getByName("localhost"),
            InetAddress.getByName("127.0.0.1")
        ).forEach {
            outpacket.address = it
            outpacket.data[0] = ++i
            socket.send(outpacket)
            sleep(100)
        }
        threadstopped = true
    }
}