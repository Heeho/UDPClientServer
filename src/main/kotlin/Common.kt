import java.net.InetAddress
import java.util.*

interface EmitterReceiver {
  private fun emit()
  private fun receive()
  private fun purge()
  fun run()
}

class Application {
  val serveraddress = InetAddress.getByName("127.0.0.1")
  val serverport = 40708
  val protocolversion = "1.0.0".hashCode()
  val sotimeout = 5000
  val lastpackettimeout = 100
  val padding = 1000
  val maxplayers = 11

  private val protocolversion = protocolversion
  private var serveraddress = serveraddress
  private var serverport = port
  private val padding = padding
  private val lastpackettimeout = lastpackettimeout
  private val insocket = DatagramSocket()
  private val inpacket = DatagramPacket(ByteArray(padding),padding)
  private val decoder = Decoder(inpacket.data)
  private val outsocket = DatagramSocket()
  private val outpacket = DatagramPacket(ByteArray(padding),padding,serveraddress,serverport)
  private val encoder = Encoder()
  private val packetmeta = SaltedPacket()
  private val connreq = ConnectionRequest()
  private val chal = Challenge()
  private val chalre = ChallengeResponse()
  private val connacc = ConnectionAccepted()
  private val connref = ConnectionRefused()
  private val serverdata = ServerData()
  private val controlack = ControlAck()
  private val keep = KeepAlive()

  init {
    insocket.soTimeout = Params.sotimeout
  }

  private fun send(p: Packet) {
    pack(encoder.reset()
      .apply{ p.serialize(this) }
      .bytes()
    )

    outsocket.send(outpacket)
  }

  private fun pack(b: ByteArray, pad: Boolean = true) {
    outpacket.length = b.size

    val d = if(pad) b.plus(ByteArray(padding - b.size))
    else b

    //encrypt data here

    outpacket.data = d
  }

  private fun newsalt() = UUID.randomUUID().hashCode()
}

open class Packet {
  enum class Type(val id: Byte) {
    CONNECTION_REQUEST(1),
    CHALLENGE(-1),
    CHALLENGE_RESPONSE(2),
    CONNECTION_ACCEPTED(-21),
    CONNECTION_REFUSED(-22),

    SERVER_STATE(-3),
    CONTROL(3),
    CONTROL_ACK(-4),
    KEEP_ALIVE(4)

    DICONNECT(5)
  }
 
  var protocolversion = Common.protocolversion
  var type = 0

  override fun serialize(e: Encoder): Encoder {
    e.write(protocolversion).write(type)
  }

  override fun deserialize(d: Decoder) {
    protocolversion = d.readInt()
    type = d.readByte()
  }
}

open class SaltedPacket: Packet {
  var salt = 0

  override fun serialize(e: Encoder): Encoder {
    super.serialize(e).write(salt)
  }

  override fun deserialize(d: Decoder) {
    super.deserialize(d)
    salt = d.readInt()
  }
}

class ConnectionRequest: SaltedPacket {
  init { type = CONNECTION_REQUEST.id }
}

class Challenge: Packet {
  init { type = CHALLENGE.id }

  var serversalt = 0

  fun serialize(e: Encoder): Encoder {
    super.serialize(e).write(serversalt)
  }

  fun deserialize(d: Decoder) {
    super.deserialize(d)
    serversalt = d.readInt()
  }
}

class ChallengeResponse: SaltedPacket {
  init { type = CHALLENGE_RESPONSE.id }
}

class ConnectionAccepted: SaltedPacket {
  init { type = CONNECTION_ACCEPTED.id }
}

class ConnectionRefused: SaltedPacket {
  init { type = CONNECTION_REFUSED.id }
}

class KeepAlive: SaltedPacket {
  init { type = KEEP_ALIVE.id }
}

class ServerState: SaltedPacket {
  init { type = SERVER_STATE.id }

  val data: Byte //test

  fun serialize(e: Encoder): Encoder {
    super.serialize(e).write(data)
  }

  fun deserialize(d: Decoder) {
    super.deserialize(d)
    data = d.readByte()
  }
}

class Control: SaltedPacket {
  init { type = CONTROL.id }

  var commanddate = 0
  var command: Byte = 0 
  
  fun serialize(e: Encoder): Encoder {
    super.serilize(e)
      .write(commanddate)
      .write(command)
  }

  fun deserialize(d: Decoder) {
    super.deserilize(d)
    commanddate = d.readInt()
    command = d.readByte()
  }
}
