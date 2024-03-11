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
        val localaddress = InetAddress.getByName("127.0.0.1")
        val serverport = 40708
        val protocolversion = "1.0.0".hashCode()
        val sotimeout = 5000
        val commandtimeout = 100
        val lastpackettimeout = 100
        val padding = 1000
        val maxclients = 11

        val emitintervalmillis = 1000L
        val receiveintervalmillis = 1000L
    }

    protected val inpacket = DatagramPacket(ByteArray(padding),padding)
    protected val decoder = Decoder(inpacket.data)
    protected val outpacket = DatagramPacket(ByteArray(padding),padding)
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
    protected val disconnect = Disconnect()

    protected var emittimestamp = System.currentTimeMillis()
    protected var receivetimestamp = System.currentTimeMillis()

    private var emitting = false
    private var receiving = false

    private val emitthread = thread(start=false) {
        emitting = true
        while(emitting) {
            emittimestamp = System.currentTimeMillis()
            emit()
            sleep(emitintervalmillis)
        }
    }
    private val receivethread = thread(start=false) {
        receiving = true
        while(receiving) {
            receivetimestamp = System.currentTimeMillis()
            //purge()
            receive()
            sleep(receiveintervalmillis)
        }
    }

    init {
        socket.soTimeout = sotimeout
    }

    fun start() {
        startemit()
        startreceive()
    }

    fun stop() {
        stopemit()
        stopreceive()
    }

    override fun dispose() {
        stop()
        try { socket.close() } finally {  }
        encoder.dispose()
        decoder.dispose()
    }

    private fun startemit() { emitthread.start() }
    private fun stopemit() { emitting = false }
    private fun startreceive() { receivethread.start() }
    private fun stopreceive() { receiving = false }

    protected open fun emit() {

    }

    protected open fun receive() {
        println("${this::class.java} received packet: ${Packet.Type.values().first { it.id == packetmeta.type }}")
    }

    protected open fun purge() {

    }

    protected fun send(p: Packet) {
        pack(encoder.reset()
            .apply{ p.serialize(this) }
            .bytes()
        )
        socket.send(outpacket)
        println("${this::class.java} sent packet: ${Packet.Type.values().first { it.id == p.type }}")
    }

    private fun pack(b: ByteArray, pad: Boolean = true) {
        outpacket.length = b.size

        val d = if(pad) b.plus(ByteArray(padding - b.size))
        else b

        //encrypt d here

        outpacket.data = d
    }

    protected fun newsalt() = UUID.randomUUID().hashCode()

    /*fun getappstatus() {
        println("--SOCKETS")
        println("${this.socket.localAddress} ${this.socket.localPort}")
        println("--DATAGRAMS")
        println("inpacket: ${inpacket.address}:${inpacket.port}")
        println("outpacket: ${outpacket.address}:${outpacket.port}")
        println("--PACKETS")
        println("connreq: $connreq")
        println("chalre: $chalre")
        println("connacc: $connacc")
        println("connref: $connref")
        println("serverstate: $serverstate")
        println("clientcom: $clientcom")
        println("clientcomack: $clientcomack")
        println("keep: $keep")
        println("disconnect: $disconnect")
    }*/
}
