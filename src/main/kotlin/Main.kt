import java.net.DatagramSocket

fun main() {
    val server = Server(DatagramSocket())
    val client = Client(DatagramSocket(55555))

    //client.send()
    //server.receive()
}