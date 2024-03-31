import common.*
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.ArrayList

class Client: Application(DatagramSocket()) {
  private val commands = ArrayList<Command>()
  private val connection: Connection = Connection().apply {
    this.receiveraddress = InetSocketAddress(serveraddress,serverport)
  }

  init {
    outpacket.socketAddress = connection.receiveraddress
    connrequest.clientpublickey = cryptor.publickey
  }

  override fun emit() {
    when(connection.state) {
      Connection.State.DISCONNECTED -> {
        connection.state = Connection.State.WAITING_CHALLENGE
        send(connrequest,null)
      }
      Connection.State.WAITING_CHALLENGE -> {
        send(connrequest,null)
      }
      Connection.State.WAITING_ACCEPTED -> {
        send(chalresponse,connection.receiverpublickey)
      }
      Connection.State.CONNECTED -> {
        if(commands.isNotEmpty())
          send(commands.first(),connection.receiverpublickey)
        if(emittimestamp - (connection.lastsendtimestamp?:0) > keepaliveinterval) {
          send(keep,connection.receiverpublickey)
          connection.lastsendtimestamp = emittimestamp
        }
      }
    }
  }

  override fun receive() {
    try {
      socket.receive(inpacket)
    } catch(ste: SocketTimeoutException) {
      println(ste)
      connection.state = Connection.State.DISCONNECTED
      return
    }

    if(badmeta()) return

    connection.lastreceivetimestamp = receivetimestamp

    when(connection.state) {
      Connection.State.WAITING_CHALLENGE -> {
        when(tokenpacket.type) {
          Packet.Type.CHALLENGE.id -> {
            challenge.deserialize(decoder)
            connection.receiverpublickey = challenge.serverpublickey
            connection.token = challenge.token
            chalresponse.token = challenge.token
            send(chalresponse,connection.receiverpublickey)
            connection.state = Connection.State.WAITING_ACCEPTED
          }
        }
      }
      Connection.State.WAITING_ACCEPTED -> {
        when(tokenpacket.type) {
          Packet.Type.CONNECTION_ACCEPTED.id -> {
            connection.state = Connection.State.CONNECTED
          }
          Packet.Type.CONNECTION_REFUSED.id -> {
            connection.state = Connection.State.DISCONNECTED
          }
        }
      }
      Connection.State.CONNECTED -> {
        when(tokenpacket.type) {
          Packet.Type.SERVER_STATE.id -> {
            serverstate.deserialize(decoder)
            //
          }
          Packet.Type.COMMAND_ACK.id -> {
            comack.deserialize(decoder)
            commands.removeIf { it.timestamp == comack.timestamp && it.id == comack.id }
          }
        }
      }
      else -> {
        println("received wrong packet: $tokenpacket")
      }
    }
  }

  fun command(b: Byte) {
    commands.add(
      Command().apply {
        this.token = connection.token?:0
        this.id = b
        this.timestamp = receivetimestamp
      }
    )
  }

  fun disconnect() {
    if(connection.state == Connection.State.CONNECTED) {
      disconnect.token = connection.token?:0
      for (i in 0..10) send(disconnect,connection.receiverpublickey)
      connection.state = Connection.State.DISCONNECTED
      stop()
    }
  }
}
