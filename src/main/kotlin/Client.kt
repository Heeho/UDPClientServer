import common.*
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.ArrayList

class Client(
  socket: DatagramSocket = DatagramSocket()
): Application(socket) {
  enum class State {
    DISCONNECTED,
    WAITING_CHALLENGE,
    WAITING_ACCEPTED,
    CONNECTED
  }

  var state = State.DISCONNECTED; private set
  var clientsalt = newsalt(); private set
  var clientid = 0; private set
  private val commands = ArrayList<ClientCommand>()

  init {
    outpacket.socketAddress = InetSocketAddress(localaddress,serverport)
    connreq.clientsalt = clientsalt
  }

  override fun purge() {
    for(c in commands) {
      if(receivetimestamp - c.commanddate > commandtimeout) {
        commands.remove(c)
      }
    }
  }

  override fun emit() {
    when(state) {
      State.CONNECTED -> {
        //if no commands to send:
        send(keep)
      }
      else -> {}
    }
  }
  override fun receive() {
    if(state == State.DISCONNECTED) {
      send(connreq)
      state = State.WAITING_CHALLENGE
      return
    }

    try {
      socket.receive(inpacket)
    } catch(e: Exception) {
      when(state) {
        State.CONNECTED -> {
          state = State.DISCONNECTED
        }
        State.WAITING_CHALLENGE -> {
          send(connreq)
        }
        State.WAITING_ACCEPTED -> {
          send(chalre)
        }
        else -> {}
      }
    }

    decoder.reset()
    packetmeta.deserialize(decoder)
    super.receive()

    if(packetmeta.protocolversion != protocolversion || packetmeta.clientsalt != clientsalt) return

    decoder.reset()
    when(state) {
      State.WAITING_CHALLENGE -> {
        when(packetmeta.type) {
          Packet.Type.CHALLENGE.id -> {
            chal.deserialize(decoder)
            chalre.clientsalt = clientsalt
            chalre.xorsalt = clientsalt xor chal.serversalt
            send(chalre)
            state = State.WAITING_ACCEPTED
          }
        }
      }
      State.WAITING_ACCEPTED -> {
        when(packetmeta.type) {
          Packet.Type.CONNECTION_ACCEPTED.id -> {
            connacc.deserialize(decoder)
            clientid = connacc.clientid
            state = State.CONNECTED
          }
          Packet.Type.CONNECTION_REFUSED.id -> {
            state = State.DISCONNECTED
          }
        }
      }
      State.CONNECTED -> {
        when(packetmeta.type) {
          Packet.Type.SERVER_STATE.id -> {
            serverstate.deserialize(decoder)
          }
          Packet.Type.CLIENT_COMMAND_ACK.id -> {
            clientcomack.deserialize(decoder)
            val c = commands.last()
            if(c.commanddate == clientcomack.commanddate && c.command == clientcomack.command)
              commands.removeLast()
          }
        }
      }
      else -> {}
    }
  }

  fun command(b: Byte) {
    commands.add(
      0,
      ClientCommand().apply {
        this.command = b
        this.commanddate = receivetimestamp
      }
    )
  }

  fun disconnect() {
    if(state == State.CONNECTED) {
      disconnect.clientsalt = clientsalt
      for (i in 0..10) send(disconnect)
      state = State.DISCONNECTED
    }
  }
}
