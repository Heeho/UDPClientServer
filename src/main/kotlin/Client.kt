class Client {
enum class States {
  DISCONNECTED,
  WAITING_CHALLENGE,
  WAITING_ACCEPTED,
  CONNECTED
}
  
  val protocolversion = Common.protocolversion
  var serveraddress = Common.serveraddress
  var serverport = Common.serverport

  val outsocket = DatagramSocket(serveraddress,serverport)
  val outpacket = DatagramPacket()
  val encoder = Encoder()

  val insocket = DatagramSocket()
  val inpacket = DatagramPacket()
  val decoder = Decoder(inpacket.data)

  var state = DISCONNECTED
  var playerid = 0
  val gamestate = GameState()

  var salt = 0

  val packetids = listOf().apply {
    Packets.values().forEach() {
      this.add(it)
    }
  }

  init {
    insocket.setSoTimeout(Common.sotimeout)
  }

  fun run() {
    emit()
    receive()
  }

  fun emit() {
    when(state) {
      DISCONNECTED -> {
        salt = Common.newsalt()
        send(CONNECTION_REQUEST)
        state = WAITING_CHALLENGE
      }
      WAITING_CHALLENGE -> {
        send(CONNECTION_REQUEST)
      }
      WAITING_ACCEPTED -> {
        send(CHALLENGE_RESPONSE)
      }
      CONNECTED -> {
        send(KEEP_ALIVE)
      }
    }
  }

  fun send(p: Packets) {
    if(!isclientpacket(p.type)) return

    pack(encoder
      .reset()
      .write(protocolversion)
      .write(p.type)
      .apply {
        when(p) {
          CONNECTION_REQUEST,
          CHALLENGE_RESPONSE,
          KEEP_ALIVE -> { it.write(salt) }
          PLAYER_CONTROL -> {}
        }
      }.bytes()
    )

    outsocket.send(outpacket)
  }

  fun isclientpacket(b: Byte) = b > 0

  fun pack(b: ByteArray, pad: Boolean = true) {
    outpacket.length = b.size
    //todo: encrypt
    if(pad)
      outpacket.data = b.plus(ByteArray(Common.padTo - b.size))
    else
      outpacket.data = b
  }

  fun receive() {
    when(state) {
      DISCONNECTED -> return
    }

    try {
      insocket.receive(inpacket)
    } catch {
      when(state) {
        CONNECTED -> { state = DISCONNECTED }
      }
      return
    }

    decoder.reset()

    if(decoder.readInt() != Common.version) return

    val packettype = decoder.readByte()
    val insalt = decoder.readInt()

    if(insalt != salt) return

    when(state) {
      WAITING_CHALLENGE -> {
        when(packettype) {
          CHALLENGE.id -> {
            salt = salt ^ decoder.readInt()
            send(CHALLENGE_RESPONSE)
            state = WAITING_ACCEPTED
          }
        }
      }
      WAITING_ACCEPTED -> {
        when(packettype) {
          CONNECTION_ACCEPTED.id -> {
            playerid = decoder.readInt()
            state = CONNECTED
          }
          CONNECTION_REFUSED.id -> {
            state = DISCONNECTED
          }
        }
      }
      CONNECTED -> {
        if(packettype == GAME_STATE.id) {
          gamestate.deserialize(decoder)
        }
      }
    }
  }
}
