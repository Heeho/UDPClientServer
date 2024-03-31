package common

import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.*
import kotlin.concurrent.thread

open class Application(
    protected val socket: DatagramSocket
): Disposable {
    companion object {
        val serveraddress = InetAddress.getLocalHost()
        val serverport = 40708
        val protocolversion = "1.0.0".hashCode()
        val padding = 1000
        val maxconnections = 11

        val emitintervalmillis = 60L
        val receiveintervalmillis = 60L

        val commandtimeout = emitintervalmillis*3
        val keepaliveinterval: Int = receiveintervalmillis.toInt()*15
        val receivetimeout: Int = keepaliveinterval*5
    }

    protected val inpacket = DatagramPacket(ByteArray(padding), padding)
    protected val decoder = Decoder(inpacket.data)
    protected val outpacket = DatagramPacket(ByteArray(padding), padding)
    protected val encoder = Encoder()

    protected val cryptor = Cryptor()

    protected val tokenpacket = TokenPacket()
    protected val connrequest = ConnectionRequest()
    protected val challenge = Challenge()
    protected val chalresponse = ChallengeResponse()
    protected val connaccepted = ConnectionAccepted()
    protected val connrefused = ConnectionRefused()
    protected val serverstate = ServerState()
    protected val command = Command()
    protected val comack = CommandAck()
    protected val keep = KeepAlive()
    protected val disconnect = Disconnect()

    protected var emittimestamp = System.currentTimeMillis()
    protected var receivetimestamp = System.currentTimeMillis()

    private var emitting = false
    private var receiving = false

    init {
        socket.soTimeout = receivetimeout
    }

    private val receivethread = thread(start = false) {
        receiving = true
        while (receiving && !socket.isClosed) {
            receivetimestamp = System.currentTimeMillis()
            try {
                receive()
            } catch (se: SocketException) {
                println("Socket was closed, receive() method interrupted.")
            }
            sleep(receiveintervalmillis)
        }
    }

    private val emitthread = thread(start = false) {
        emitting = true
        while (emitting && !socket.isClosed) {
            emittimestamp = System.currentTimeMillis()
            emit()
            sleep(emitintervalmillis)
        }
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
        socket.close()
        encoder.dispose()
        decoder.dispose()
    }

    protected open fun receive() {
        //test
    }

    protected open fun emit() {
        //test
        //println("${this::class.java} emitted data.")
    }

    protected fun badmeta(): Boolean {
        decoder.reset()
        tokenpacket.deserialize(decoder)
        decoder.reset()
        println("${this::class.java} received packet: ${Packet.Type.values().first { it.id == tokenpacket.type }}")
        return tokenpacket.protocolversion != protocolversion
    }

    protected fun send(p: Packet, encryptionkey: Long?) {
        pack(
            encoder.reset().apply{ p.serialize(this) }.bytes(),
            encryptionkey,
            p.requiresserverresponse
        )
        socket.send(outpacket)
        //test
        println("${this::class.java} sent packet: ${Packet.Type.values().first { it.id == p.type }}")
    }

    private fun pack(b: ByteArray, encryptionkey: Long?, pad: Boolean) {
        val d = if(pad) b.plus(ByteArray(padding - b.size)) else b

        if(encryptionkey != null)
            cryptor.encrypt(d,encryptionkey)
        cryptor.sign(d)

        outpacket.data = d
        outpacket.length = d.size
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
        println("connreq: $connrequest")
        println("chal: $challenge")
        println("chalre: $chalresponse")
        println("connacc: $connaccepted")
        println("connref: $connrefused")
        println("serverstate: $serverstate")
        println("clientcom: $command")
        println("clientcomack: $comack")
        println("keep: $keep")
        println("disconnect: $disconnect")
    }

    fun lastcommandcommand() = command.id
    fun lastcomackcommand() = comack.id
    fun lastkeepalivetoken() = keep.token
    fun lastserverstatetimestamp() = serverstate.timestamp
}
