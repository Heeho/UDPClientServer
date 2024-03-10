import common.Application
import common.Packet
import java.net.InetAddress
import java.net.InetSocketAddress

class Server: Application() {
  private val address = Array<InetAddress?>(maxclients) { null }
  private val clientsalt = Array(maxclients) { 0 }
  private val serversalt = Array(maxclients) { 0 }
  private val salt = Array(maxclients) { 0 }
  private val connected = Array(maxclients) { false }
  private val lastpacketdate = Array<Long>(maxclients) { 0 }

  init {
    socket.bind(InetSocketAddress(localaddress,serverport))
  }

  override fun emit() {
    for(i in 0 until maxclients) {
      if(connected[i]) {
        outpacket.address = address[i]
        send(serverstate)
      }
    }
  }

  override fun purge() {
    for(i in 0 until maxclients) {
      if(clientsalt[i] != 0 && receivetimestamp - (lastpacketdate[i]) > lastpackettimeout) {
        initslot(i)
      }
    }
  }

  private fun initslot(i: Int) {
    address[i] = null
    connected[i] = false
    clientsalt[i] = 0
    serversalt[i] = 0
    salt[i] = 0
    lastpacketdate[i] = 0
  }

  override fun receive() {
    decoder.reset()

    try {
      socket.receive(inpacket)
    } catch(e: Exception) { return }
    packetmeta.deserialize(decoder)

    if(packetmeta.protocolversion != protocolversion) return
    outpacket.address = inpacket.address
    outpacket.port = inpacket.port
    decoder.reset()

    var i = getclientid(packetmeta.salt)

    if(i < 0) {
      when(packetmeta.type) {
        Packet.Type.CONNECTION_REQUEST.id -> {
          i = getclientid(0)
          if(i > 0) {
            connreq.deserialize(decoder)
            clientsalt[i] = connreq.salt
            serversalt[i] = newsalt()
            salt[i] = clientsalt[i] xor serversalt[i]
            chal.salt = clientsalt[i]
            chal.serversalt = serversalt[i]
            send(chal)
          } else {
            connref.salt = connreq.salt
            send(connref)
          }
        }
        Packet.Type.CHALLENGE_RESPONSE.id -> {
          chalre.deserialize(decoder)
          connref.salt = chalre.salt
          send(connref)
        }
        else -> {}
      }
    } else {
      when(packetmeta.type) {
        Packet.Type.CHALLENGE_RESPONSE.id -> {
          chalre.deserialize(decoder)
          connected[i] = true
          connacc.salt = chalre.salt
          send(connacc)
        }
        Packet.Type.KEEP_ALIVE.id -> {
          keep.deserialize(decoder)
          getclientid(keep.salt)
        }
        Packet.Type.CLIENT_COMMAND.id -> {
          clientcom.deserialize(decoder)
          clientcomack.command = clientcom.command
          clientcomack.commanddate = clientcom.commanddate
          send(clientcomack)
        }
        Packet.Type.DISCONNECT.id -> {
          initslot(i)
        }
        else -> {}
      }
    }
  }

  fun getclientid(salt: Int): Int {
    val i = this.salt.indexOf(salt)
    if(i >= 0)  {
      address[i] = inpacket.address
      lastpacketdate[i] = receivetimestamp
    }
    return i
  }

  fun getclientcount() = connected.count { it }
}
