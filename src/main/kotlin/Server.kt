import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class Server {
  private val protocolversion = Params.protocolversion
  private val lastpackettimeout = Params.lastpackettimeout

  private val insocket = DatagramSocket(Params.port,Params.serveraddress)
  private val inpacket = DatagramPacket(ByteArray(Params.padding),Params.padding)
  private val decoder = Decoder(inpacket.data)

  private val outsocket = DatagramSocket()
  private val outpacket = DatagramPacket(ByteArray(Params.padding),Params.padding)
  private val encoder = Encoder()

  private val gamestate = GameState()

  private val maxclients = Params.maxplayers
  private val address = Array<InetAddress?>(maxclients) { null }
  private val clientsalt = Array(maxclients) { 0 }
  private val serversalt = Array(maxclients) { 0 }
  private val salt = Array(maxclients) { 0 }
  private val connected = Array(maxclients) { false }
  private val lastpacketdate = Array<Long>(maxclients) { 0 }

  fun run() {
    broadcast()
    receive()
    purge()
  }

  private fun broadcast() {
    for(i in 0 until maxclients) {
      if(connected[i]) {
        outpacket.address = address[i]
        send(Packets.GAME_STATE)
      }
    }
  }

  private fun purge() {
    for(i in 0 until maxclients) {
      if(clientsalt[i] > 0 && System.currentTimeMillis() - (lastpacketdate[i]) > lastpackettimeout) {
        initslot(i)
      }
    }
  }

  private fun receive() {
    try {
      insocket.receive(inpacket)
    } catch(e: Exception) { return }

    decoder.reset()

    if(decoder.readInt() != protocolversion) return

    val packettype = decoder.readByte()
    val insalt = decoder.readInt()

    outpacket.address = inpacket.address

    when(packettype) {
      Packets.CONNECTION_REQUEST.id -> {
        val i = clientsalt.indexOf(0)
        if(i < 0) {
          send(Packets.CONNECTION_REFUSED)
        } else {
          address[i] = inpacket.address
          clientsalt[i] = insalt
          serversalt[i] = Utils.newsalt()
          lastpacketdate[i] = System.currentTimeMillis()
          send(Packets.CHALLENGE)
        }
      }
      Packets.CHALLENGE_RESPONSE.id -> {
        val i = salt.indexOf(insalt)
        if (i < 0)  {
          send(Packets.CONNECTION_REFUSED)
        } else {
          address[i] = inpacket.address
          connected[i] = true
          lastpacketdate[i] = System.currentTimeMillis()
          send(Packets.CONNECTION_ACCEPTED)
        }
      }
      Packets.KEEP_ALIVE.id -> {
        val i = salt.indexOf(insalt)
        if (i >= 0)  {
          address[i] = inpacket.address
          lastpacketdate[i] = System.currentTimeMillis()
        }
      }
      Packets.PLAYER_CONTROL.id -> {}
      else -> {}
    }
  }

  private fun initslot(i: Int) {
    if(i < 0) return

    address[i] = null
    connected[i] = false
    clientsalt[i] = 0
    serversalt[i] = 0
    salt[i] = 0
    lastpacketdate[i] = 0
  }

  private fun send(p: Packets) {
    val i = address.indexOf(outpacket.address)
    if(i < 0) return

    pack(encoder
      .reset()
      .write(protocolversion)
      .write(p.id)
      .apply {
        when(p) {
          Packets.CHALLENGE -> { this.write(clientsalt[i]).write(serversalt[i]) }
          Packets.CONNECTION_ACCEPTED -> { this.write(salt[i]).write(i) }
          Packets.CONNECTION_REFUSED -> { this.write(clientsalt[i]) }
          Packets.GAME_STATE -> { gamestate.serialize(this) }
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
}
