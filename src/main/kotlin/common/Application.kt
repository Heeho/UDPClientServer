package common

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*

open class Application {
    companion object {
        val serveraddress = InetAddress.getByName("127.0.0.1")
        val serverport = 40708
        val protocolversion = "1.0.0".hashCode()
        val sotimeout = 5000
        val commandtimeout = 100
        val lastpackettimeout = 100
        val padding = 1000
        val maxclients = 11
    }

    protected val insocket = DatagramSocket()
    protected val inpacket = DatagramPacket(ByteArray(padding),padding)
    protected val decoder = Decoder(inpacket.data)
    protected val outsocket = DatagramSocket()
    protected val outpacket = DatagramPacket(ByteArray(padding),padding,serveraddress,serverport)
    protected val encoder = Encoder()

    protected val packetmeta = SaltedPacket()
    protected val connreq = ConnectionRequest()
    protected val chal = Challenge()
    protected val chalre = ChallengeResponse()
    protected val connacc = ConnectionAccepted()
    protected val connref = ConnectionRefused()
    protected val serverstate = ServerState()
    protected val clientcom = ClientCommand()
    protected val clientcomack = ClientCommandAck()
    protected val keep = KeepAlive()

    protected var timestamp = 0L

    init {
        insocket.soTimeout = sotimeout
    }

    fun run() {
        timestamp = System.currentTimeMillis()
        emit()
        receive()
        purge()
    }

    protected open fun emit() {}
    protected open fun receive() {}
    protected open fun purge() {}

    protected fun send(p: Packet) {
        pack(encoder.reset()
            .apply{ p.serialize(this) }
            .bytes()
        )

        outsocket.send(outpacket)
    }

    protected fun pack(b: ByteArray, pad: Boolean = true) {
        outpacket.length = b.size

        val d = if(pad) b.plus(ByteArray(padding - b.size))
        else b

        //encrypt data here

        outpacket.data = d
    }

    protected fun newsalt() = UUID.randomUUID().hashCode()
}
