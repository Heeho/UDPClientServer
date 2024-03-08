import java.net.DatagramPacket
import java.net.DatagramSocket

class Client {
  private enum class States {
    DISCONNECTED,
    WAITING_CHALLENGE,
    WAITING_ACCEPTED,
    CONNECTED
  }

  private val protocolversion = Params.protocolversion
  private var serveraddress = Params.serveraddress
  private var serverport = Params.port

  private val outsocket = DatagramSocket()
  private val outpacket = DatagramPacket(ByteArray(Params.padding),Params.padding,serveraddress,serverport)
  private val encoder = Encoder()

  private val insocket = DatagramSocket()
  private val inpacket = DatagramPacket(ByteArray(Params.padding),Params.padding)
  private val decoder = Decoder(inpacket.data)

  private var state = States.DISCONNECTED
  private var salt = 0
  private var playerid = 0
  private val gamestate = GameState()

  init {
    insocket.soTimeout = Params.sotimeout
  }

  fun run() {
    emit()
    receive()
  }

  private fun emit() {
    when(state) {
      States.DISCONNECTED -> {
        salt = Utils.newsalt()
        send(Packets.CONNECTION_REQUEST)
        state = States.WAITING_CHALLENGE
      }
      States.WAITING_CHALLENGE -> {
        send(Packets.CONNECTION_REQUEST)
      }
      States.WAITING_ACCEPTED -> {
        send(Packets.CHALLENGE_RESPONSE)
      }
      States.CONNECTED -> {
        send(Packets.KEEP_ALIVE)
      }
    }
  }

  private fun send(p: Packets) {
    pack(encoder
      .reset()
      .write(protocolversion)
      .write(p.id)
      .apply {
        when(p) {
          Packets.CONNECTION_REQUEST,
          Packets.CHALLENGE_RESPONSE,
          Packets.KEEP_ALIVE -> { this.write(salt) }
          Packets.PLAYER_CONTROL -> {}
          else -> {}
        }
      }.bytes()
    )

    outsocket.send(outpacket)
  }

  private fun pack(b: ByteArray, pad: Boolean = true) {
    outpacket.length = b.size
    //encrypt bytes
    if(pad)
      outpacket.data = b.plus(ByteArray(Params.padding - b.size))
    else
      outpacket.data = b
  }

  private fun receive() {
    if(state == States.DISCONNECTED) return

    try {
      insocket.receive(inpacket)
    } catch(e: Exception) {
      if(state == States.CONNECTED) { state = States.DISCONNECTED }
      return
    }

    decoder.reset()

    if(decoder.readInt() != Params.protocolversion) return

    val packettype = decoder.readByte()
    val insalt = decoder.readInt()

    if(insalt != salt) return

    when(state) {
      States.WAITING_CHALLENGE -> {
        when(packettype) {
          Packets.CHALLENGE.id -> {
            salt = salt xor decoder.readInt()
            send(Packets.CHALLENGE_RESPONSE)
            state = States.WAITING_ACCEPTED
          }
        }
      }
      States.WAITING_ACCEPTED -> {
        when(packettype) {
          Packets.CONNECTION_ACCEPTED.id -> {
            playerid = decoder.readInt()
            state = States.CONNECTED
          }
          Packets.CONNECTION_REFUSED.id -> {
            state = States.DISCONNECTED
          }
        }
      }
      States.CONNECTED -> {
        if(packettype == Packets.GAME_STATE.id) {
          gamestate.deserialize(decoder)
        }
      }
      else -> {}
    }
  }
}
