import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

//WIP

fun main() {
    val cpacket = DatagramPacket(ByteArray(100),100, InetAddress.getByName("127.0.0.1"),40708)
    val spacket = DatagramPacket(ByteArray(100),5)
    val csocket = DatagramSocket()

    val ssocket = DatagramSocket(40708)

    val baos = ByteArrayOutputStream(5)
    val dos = DataOutputStream(baos)

    baos.reset()
    dos.writeInt(11111)
    dos.writeByte(15)
    dos.writeByte(30)
    dos.flush()

    val data = baos.toByteArray()
    data.forEach {
        println(it)
    }

    cpacket.data = baos.toByteArray()

    cpacket.length = 2
    csocket.send(cpacket)

    ssocket.receive(spacket)
}