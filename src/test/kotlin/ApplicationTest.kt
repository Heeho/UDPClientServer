import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.Thread.sleep

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    private var client = Client()
    private var server = Server()

    @BeforeEach
    fun starttest() {
        client.dispose()
        server.dispose()
        server = Server()
        client = Client()
        server.start()
        client.start()
        sleep(1000)
    }

    @Test
    fun connect() {
        assertEquals(server.connections().first().state,Connection.State.CONNECTED)
    }

    @Test
    fun serverstate() {
        assertEquals(client.lastserverstatetimestamp(),server.lastserverstatetimestamp())
    }

    @Test
    fun command() {
        client.command(1)
        sleep(1000)
        assertEquals(server.lastcomackcommand(),server.lastcommandcommand())
        assertEquals(client.lastcomackcommand(),server.lastcomackcommand())
    }

    @Test
    fun keepalive() {
        client.command(1)
        sleep(1000)
        assertEquals(client.lastkeepalivetoken(),server.lastkeepalivetoken())
    }

    @Test
    fun disconnect() {
        client.disconnect()
        sleep(1000)
        assert(server.connections().isEmpty())
    }
}