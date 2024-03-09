import common.Application
import common.Packet
import java.net.InetAddress

class Server: Application() {
  private val address = Array<InetAddress?>(maxclients) { null }
  private val clientsalt = Array(maxclients) { 0 }
  private val serversalt = Array(maxclients) { 0 }
  private val salt = Array(maxclients) { 0 }
  private val connected = Array(maxclients) { false }
  private val lastpacketdate = Array<Long>(maxclients) { 0 }

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
      if(clientsalt[i] != 0 && timestamp - (lastpacketdate[i]) > lastpackettimeout) {
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
    try {
      insocket.receive(inpacket)
    } catch(e: Exception) { return }

    decoder.reset()
    packetmeta.deserialize(decoder)
    if(packetmeta.protocolversion != protocolversion) return

    outpacket.address = inpacket.address
    decoder.reset()

    when(packetmeta.type) {
      Packet.Type.CONNECTION_REQUEST.id -> {
        connreq.deserialize(decoder)
        val i = update(0)
        if(i > 0) {
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
        val i = update(chalre.salt)
        if(i > 0)  {
          connected[i] = true
          connacc.salt = chalre.salt
          send(connacc)
        } else {
          connref.salt = chalre.salt
          send(connref)
        }
      }
      Packet.Type.KEEP_ALIVE.id -> {
        keep.deserialize(decoder)
        update(keep.salt)
      }
      Packet.Type.CLIENT_COMMAND.id -> {
        clientcom.deserialize(decoder)
        if(update(clientcom.salt) > 0) {
          clientcomack.command = clientcom.command
          clientcomack.commanddate = clientcom.commanddate
          send(clientcomack)
        }
      }
      else -> {}
    }
  }

  fun update(s: Int): Int {
    val i = salt.indexOf(s)
    if(i >= 0)  {
      address[i] = inpacket.address
      lastpacketdate[i] = timestamp
    }
    return i
  }
}
