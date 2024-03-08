import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket

class Client {
    private val serveraddress = Common.serveraddress
    private val serverport = Common.serverport
    private val socket = DatagramSocket()

    private val clientpacket = DatagramPacket(ByteArray(Common.maxPacketSize),0,serveraddress,serverport)
    private val ostream = ByteArrayOutputStream()
    private val odatastream = DataOutputStream(ostream)

    private val istream = ByteArrayInputStream(ByteArray(Common.maxPacketSize))
    private val idatastream = DataInputStream(istream)

    fun send(p: Packet, pad: Boolean) {
        val data = serialize(p)

        clientpacket.data = data
        clientpacket.length = if(pad) Common.clientPacketPadding else data.size

        socket.send(clientpacket)
    }

    private fun serialize(p: Packet): ByteArray {
        ostream.flush()
        ostream.reset()
        p.serialize(odatastream)
        val data = ostream.toByteArray()
        return data
    }
}