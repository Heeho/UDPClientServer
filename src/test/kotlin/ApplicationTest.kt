import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.Thread.sleep

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    private var client = Client()
    private var server = Server()

    @BeforeEach
    fun inittest() {
        client.dispose()
        server.dispose()
        client = Client()
        server = Server()
    }

    @Test
    fun connect() {
        server.start()
        client.start()

        sleep(7000)
        client.command(1)
        sleep(5000)
        client.disconnect()

        client.dispose()
        server.dispose()
    }
}