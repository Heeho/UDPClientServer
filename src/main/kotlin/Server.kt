class Server {
  val protocolversion = Common.protocolversion
  //var serveraddress = Common.serveraddress
  //var serverport = Common.serverport
  val lastpackettimeout = Common.lastpackettimeout

  val insocket = DatagramSocket()
  val inpacket = DatagramPacket()
  val decoder = Decoder(inpacket.data)

  val outsocket = DatagramSocket()
  val outpacket = DatagramPacket()
  val encoder = Encoder()

  val gamestate = GameState()

  val maxclients = Common.servermaxplayers
  val address = Array<InetAddress?>(maxclients)
  val clientsalt = Array<Int?>(maxclients)
  val serversalt = Array<Int?>(maxclients)
  val salt = Array<Int?>(maxclients)
  val connected = Array<Boolean>(maxclients)
  val lastpocketdate = Array<Int?>(maxclients)

  fun run() {
    broadcast()
    receive()
    purge()
  }

  fun broadcast() {
    for(var i = 0; i < maxclients; i++) {
      if(connected[i]) {
        outpacket.address = address[i]
        send(GAME_STATE)
      }
    }
  }

  fun purge() {
    for(var i = 0; i < maxclients; i++) {
      if(Now() - lastpacketdate[i]?:0 > lastpackettimeout) {
        initslot(i)
      }
    }
  }

  fun receive() {
    try {
      insocket.receive(inpacket)
    } catch { return }

    decoder.reset()

    if(decoder.readInt() != protocolversion) return

    val packettype = decoder.readByte()
    val insalt = decoder.readInt()

    outpacket.address = inpacket.address

    when(packettype) {
      CONNECTION_REQUEST.id -> {
        val i = clientsalt.indexOf(null)
        if(i < 0) {
          send(CONNECTION_REFUSED)
        } else {
          address[i] = inpacket.address
          clientsalt[i] = insalt
          serversalt[i] = Common.newsalt()
          lastpacket[i] = Now()
          send(CHALLENGE)
        }
      }
      CHALLENGE_RESPONSE.id -> {
        val i = salt.indexOf(insalt)
        if (i < 0)  {
          send(CONNECTION_REFUSED)
        } else {
          address[i] = inpacket.address
          connected[i] = true
          lastpacket[i] = Now()
          send(CONNECTION_ACCEPTED)
        }
      }
      KEEP_ALIVE.id -> {
        val i = salt.indexOf(insalt)
        if not(i < 0)  {
          address[i] = inpacket.address
          lastpacket[i] = Now()
        }
      }
      PLAYER_CONTROL.id -> {
        
      }
      else -> {}
    }
  }

  fun initslot(i: Int) {
    if(i < 0) return

    address[i] = null
    connected[i] = false
    clientsalt[i] = null
    serversalt[i] = null
    salt[i] = null
    lastpacket[i] = null
  }

  fun isclientpacket(b: Byte) = b > 0

  fun send(p: Packets) {
    (isclientpacket(p.type)) return

    val i = address.indexOf(outpacket.address)
    if(i < 0) return

    pack(encoder
      .reset()
      .write(protocolversion)
      .write(p.type)
      .apply {
        when(p) {
          CHALLENGE -> { it.write(clientsalt[i]).write(serversalt[i]) }
          CONNECTION_ACCEPTED -> { it.write(salt[i]).write(i) }
          CONNECTION_REFUSED -> { it.write(clientsalt[i]) }
          GAME_STATE -> { gamestate.serialize(it) }
        }
      }.bytes()
    )

    outsocket.send(outpacket)
  }
}
