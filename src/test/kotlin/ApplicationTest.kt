import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.Thread.sleep

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    private var client: Client = Client()
    private var server: Server = Server()

    @BeforeEach
    fun inittest() {
        client = Client()
        server = Server()
    }

    @Test
    fun connect() {
        server.start()
        client.start()

        while(server.getclientcount() == 0) {
            println("\n--STATUS")
            println("\n--CLIENT")
            println(client.state)
            client.getappstatus()
            println("\n--SERVER")
            server.getappstatus()
            sleep(2000)
        }
        client.dispose()
        server.dispose()
    }
}