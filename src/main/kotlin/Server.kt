import common.Application
import common.Packet
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import kotlin.collections.ArrayList

class Server: Application(DatagramSocket(serverport)) {
  private val connections = ArrayList<Connection>()

  override fun emit() {
    serverstate.timestamp = emittimestamp
    for(connection in connections.shuffled()) {
      when(connection.state) {
        Connection.State.CONNECTED -> {
          send(serverstate,connection.receiverpublickey)
        }
        else -> {}
      }
    }
  }

  private fun purge() {
    connections.removeIf { receivetimestamp - (it.lastreceivetimestamp?:0) > receivetimeout }
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

    outpacket.address = inpacket.address
    outpacket.port = inpacket.port

    val connection = connections.find { c -> c.receiveraddress?.address == inpacket.address }

    if(connection == null) {
      when(tokenpacket.type) {
        Packet.Type.CONNECTION_REQUEST.id -> {
          if(connections.size < maxconnections) {
            connrequest.deserialize(decoder)
            val newtoken = newtoken()
            val newconnection = Connection().apply {
              this.token = newtoken
              this.receiveraddress = InetSocketAddress(inpacket.address,inpacket.port)
              this.receiverpublickey = connrequest.clientpublickey
              this.lastreceivetimestamp = receivetimestamp
            }
            connections.add(newconnection)
            challenge.token = newtoken
            challenge.serverpublickey = cryptor.publickey
            send(challenge,newconnection.receiverpublickey)
          } else {
            //если connref, то как избежать спуффинга адреса получателя? токен появляется, начиная с chal
            //connref имеет смысл, только если клиенту заранее известен публичный ключ сервера, и можно на нем зашифровать некий идентификатор
            //легче просто не отвечать и надеяться на таймаут клиента
          }
        }
        else -> {}
      }
    } else {
      when(connection.state) {
        Connection.State.CONNECTED -> {
          connection.lastreceivetimestamp = receivetimestamp
          when (tokenpacket.type) {
            Packet.Type.COMMAND.id -> {
              command.deserialize(decoder)
              if(connection.token != command.token) return
              comack.token = connection.token?:0
              comack.id = command.id
              comack.timestamp = command.timestamp
              send(comack,connection.receiverpublickey)
            }
            Packet.Type.DISCONNECT.id -> {
              disconnect.deserialize(decoder)
              if(connection.token != disconnect.token) return
              connections.remove(connection)
            }

            else -> {
              //println("received wrong packet: $packetmeta")
            }
          }
        }
        else -> {
          when (tokenpacket.type) {
            Packet.Type.CONNECTION_REQUEST.id -> {
              connrequest.deserialize(decoder)
              challenge.token = connection.token?:0
              challenge.serverpublickey = cryptor.publickey
              send(challenge,connection.receiverpublickey)
            }
            Packet.Type.CHALLENGE_RESPONSE.id -> {
              chalresponse.deserialize(decoder)
              if (chalresponse.token == connection.token) {
                connection.state = Connection.State.CONNECTED
                connaccepted.token = chalresponse.token
                send(connaccepted,connection.receiverpublickey)
                connection.state = Connection.State.CONNECTED
              } else {
                connrefused.token = chalresponse.token
                send(connrefused,connection.receiverpublickey)
              }
            }

            else -> {
              //println("received wrong packet, client is already connected: $packetmeta")
            }
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

  fun connections() = connections.toList()
}
