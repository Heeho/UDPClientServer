import common.Application
import common.Packet
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import kotlin.collections.ArrayList

class Server(
  socket: DatagramSocket = DatagramSocket(serverport)
): Application(socket) {
  val serverprivatekey: Long
  val serverpublickey: Long

  init {
    serverprivatekey = 1
    serverpublickey = 1
  }

  private val connections = ArrayList<Connection>()

  override fun emit() {
    for(c in connections.shuffled()) {
      if(c.connected) {
        outpacket.address = c.address
        send(serverstate)
      }
    }
    super.emit()
  }

  private fun purge() {
    connections.removeIf { receivetimestamp - (it.lastpackettimestamp) > lastpackettimeout }
  }

  override fun receive() {
    purge()

    try {
      socket.receive(inpacket)
    } catch(ste: SocketTimeoutException) {
      println(ste)
      return
    }

    if(badmeta()) return
    super.receive()

    outpacket.address = inpacket.address
    outpacket.port = inpacket.port


    val connection = connections.find { c -> c.address == inpacket.address }

    if(connection == null) {
      when(packetmeta.type) {
        Packet.Type.CONNECTION_REQUEST.id -> {
          if(connections.size >= maxconnections)
            send(connref)
          else {
            connreq.deserialize(decoder)
            val c = Connection(
              inpacket.address,
              connreq.clientpublickey,
              newtoken(),
              false,
              receivetimestamp
            )
            connections.add(c)
            chal.token = c.token
            chal.serverpublickey = serverpublickey
            send(chal)
          }
        }
        else -> {}
      }
    } else {
      if(connection.connected) {
        connection.lastpackettimestamp = receivetimestamp
        when(packetmeta.type) {
          Packet.Type.COMMAND.id -> {
            command.deserialize(decoder)
            comack.token = connection.token
            comack.command = command.command
            comack.commanddate = command.commanddate
            send(comack)
          }
          Packet.Type.DISCONNECT.id -> {
            connections.remove(connection)
          }
          else -> {
            //println("received wrong packet: $packetmeta")
          }
        }
      } else {
        when(packetmeta.type) {
          Packet.Type.CONNECTION_REQUEST.id -> {
            connreq.deserialize(decoder)
            chal.token = connection.token
            chal.serverpublickey = serverpublickey
            send(chal)
          }
          Packet.Type.CHALLENGE_RESPONSE.id -> {
            chalre.deserialize(decoder)
            if (chalre.token == connection.token) {
              connection.connected = true
              connacc.token = chalre.token
              send(connacc)
            } else {
              send(connref)
            }
          }
          else -> {
            //println("received wrong packet, client is already connected: $packetmeta")
          }
        }
      }
    }
  }

  override fun getappstatus() {
    super.getappstatus()
    println("--CONNECTIONS")
    connections.forEach { println(it) }
  }
  //fun getclientcount() = connected.count { it }
}
