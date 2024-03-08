import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket

class Server {
    val maxConnections = 10
    val numConnections = 0
    val address = Array(maxConnections) { "" }
    val connected = Array(maxConnections) { false }
    val salt = Array(maxConnections) { 0 }
    val socket = DatagramSocket(Common.serverport)

    val serverpacket = DatagramPacket(ByteArray(Common.maxPacketSize),Common.maxPacketSize)
    private val ostream = ByteArrayOutputStream()
    private val odatastream = DataOutputStream(ostream)

    val clientpacket = DatagramPacket(ByteArray(Common.maxPacketSize),Common.maxPacketSize)
    private val istream = ByteArrayInputStream(clientpacket.data)
    private val idatastream = DataInputStream(istream)

    fun run() {
        socket.receive(clientpacket)
        istream.reset()

        val version = idatastream.readInt()
        if(version != Common.version) return

        val type = idatastream.readByte().toInt()
        when(type) {
            Common.CONNECTION_REQUEST -> {
                val clientsalt = idatastream.readInt()
            }
            else -> return
        }
    }

    fun challenge(clientsalt: Int) {

    }
}