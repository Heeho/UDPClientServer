package common

import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import kotlin.concurrent.thread

open class Application(
    protected val socket: DatagramSocket
): Disposable {
    companion object {
        val localaddress = InetAddress.getLocalHost()
        val serverport = 40708
        val protocolversion = "1.0.0".hashCode()
        val sotimeout = 5000
        val padding = 1000
        val maxconnections = 11

        val emitintervalmillis = 60L
        val receiveintervalmillis = 60L
        val commandtimeout = 100
        val lastpackettimeout = 5000L
        val keepaliveinterval = 1000L
    }

    protected val inpacket = DatagramPacket(ByteArray(padding), padding)
    protected val decoder = Decoder(inpacket.data)
    protected val outpacket = DatagramPacket(ByteArray(padding), padding)
    protected val encoder = Encoder()

    protected val packetmeta = TokenPacket()
    protected val connreq = ConnectionRequest()
    protected val chal = Challenge()
    protected val chalre = ChallengeResponse()
    protected val connacc = ConnectionAccepted()
    protected val connref = ConnectionRefused()
    protected val serverstate = ServerState()
    protected val command = Command()
    protected val comack = CommandAck()
    protected val keep = KeepAlive()
    protected val disconnect = Disconnect()

    protected var emittimestamp = System.currentTimeMillis()
    protected var receivetimestamp = System.currentTimeMillis()

    private var emitting = false
    private var receiving = false

    private val receivethread = thread(start = false) {
        receiving = true
        while (receiving) {
            receivetimestamp = System.currentTimeMillis()
            try {
                receive()
            } catch(e: Exception) {
                println(e)
            }
            sleep(receiveintervalmillis)
        }
    }

    private val emitthread = thread(start = false) {
        emitting = true
        while (emitting) {
            emittimestamp = System.currentTimeMillis()
            try {
                emit()
            } catch(e: Exception) {
                println(e)
            }
            sleep(emitintervalmillis)
        }
    }

    init {
        socket.soTimeout = sotimeout
    }

    fun start() {
        receivethread.start()
        emitthread.start()
    }

    open fun stop() {
        receiving = false
        emitting = false
    }

    override fun dispose() {
        stop()
        try {
            socket.close()
        } catch(e: Exception) {
            println(e)
        }
        encoder.dispose()
        decoder.dispose()
    }

    protected open fun receive() {
        //test
        println("${this::class.java} received packet: ${Packet.Type.values().first { it.id == packetmeta.type }}")
    }

    protected open fun emit() {
        //test
        println("${this::class.java} emitted data.")
    }

    protected fun badmeta(): Boolean {
        decoder.reset()
        packetmeta.deserialize(decoder)
        decoder.reset()
        return packetmeta.protocolversion != protocolversion
    }

    protected fun send(p: Packet) {
        pack(encoder.reset()
            .apply{ p.serialize(this) }
            .bytes()
        )
        socket.send(outpacket)
        //test
        println("${this::class.java} sent packet: ${Packet.Type.values().first { it.id == p.type }}")
    }

    private fun pack(b: ByteArray, pad: Boolean = true) {
        outpacket.length = b.size

        val d = if(pad) b.plus(ByteArray(padding - b.size)) else b

        //encrypt d here

        outpacket.data = d
    }

    protected fun newtoken() = UUID.randomUUID().hashCode()

    open fun getappstatus() {
        println("===========================")
        println("----${this::class.java}----")
        println("===========================")
        println("--SOCKETS")
        println("${this.socket.localAddress} ${this.socket.localPort}")
        println("--DATAGRAMS")
        println("inpacket: ${inpacket.address}:${inpacket.port}")
        println("outpacket: ${outpacket.address}:${outpacket.port}")
        println("--PACKETS")
        println("connreq: $connreq")
        println("chal: $chal")
        println("chalre: $chalre")
        println("connacc: $connacc")
        println("connref: $connref")
        println("serverstate: $serverstate")
        println("clientcom: $command")
        println("clientcomack: $comack")
        println("keep: $keep")
        println("disconnect: $disconnect")
    }
}
