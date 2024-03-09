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
      if(clientsalt[i] > 0 && System.currentTimeMillis() - (lastpacketdate[i]) > lastpackettimeout) {
        initslot(i)
      }
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
        val i = clientsalt.indexOf(0)
        if(i >= 0) {
          address[i] = inpacket.address
          clientsalt[i] = connreq.salt
          serversalt[i] = newsalt()
          salt[i] = clientsalt[i] xor serversalt[i]
          lastpacketdate[i] = System.currentTimeMillis()
          chal.salt = connreq.salt
          chal.serversalt = serversalt[i]
          send(chal)
        } else {
          connref.salt = connreq.salt
          send(connref)
        }
      }
      Packet.Type.CHALLENGE_RESPONSE.id -> {
        chalre.deserialize(decoder)
        val i = salt.indexOf(chalre.salt)
        if (i >= 0)  {
          address[i] = inpacket.address
          lastpacketdate[i] = System.currentTimeMillis()
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
        val i = salt.indexOf(keep.salt)
        if (i >= 0)  {
          address[i] = inpacket.address
          lastpacketdate[i] = System.currentTimeMillis()
        }
      }
      Packet.Type.CLIENT_COMMAND.id -> {
        clientcom.deserialize(decoder)
        val i = salt.indexOf(clientcom.salt)
        if (i >= 0)  {
          address[i] = inpacket.address
          lastpacketdate[i] = System.currentTimeMillis()
          clientcomack.command = clientcom.command
          clientcomack.commanddate = clientcom.commanddate
          send(clientcomack)
        }
      }
      else -> {}
    }
  }
}
