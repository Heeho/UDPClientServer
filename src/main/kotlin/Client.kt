import common.*
import java.net.InetSocketAddress
import java.util.ArrayList

class Client: Application() {
  enum class State {
    DISCONNECTED,
    WAITING_CHALLENGE,
    WAITING_ACCEPTED,
    CONNECTED
  }

  var state = State.DISCONNECTED; private set
  var salt = 0; private set
  var clientid = 0; private set
  private val commands = ArrayList<ClientCommand>()

  init {
    socket.bind(InetSocketAddress(localaddress,0))
    outpacket.socketAddress = InetSocketAddress(localaddress,serverport)
  }

  override fun emit() {
    when(state) {
      State.DISCONNECTED -> {
        salt = newsalt()
        connreq.salt = salt
        send(connreq)
        state = State.WAITING_CHALLENGE
      }
      State.WAITING_CHALLENGE -> {
        send(connreq)
      }
      State.WAITING_ACCEPTED -> {
        send(chalre)
      }
      State.CONNECTED -> {
        //if(!sendcontrol())
        send(keep)
      }
    }
  }

  override fun purge() {
    for(c in commands) {
      if(receivetimestamp - c.commanddate > commandtimeout) {
        commands.remove(c)
      }
    }
  }

  override fun receive() {
    if(state == State.DISCONNECTED) return

    try {
      socket.receive(inpacket)
    } catch(e: Exception) {
      if(state == State.CONNECTED) {
        state = State.DISCONNECTED
      }
      return
    }

    decoder.reset()
    packetmeta.deserialize(decoder)

    println("client received packet:")
    println(packetmeta.protocolversion)
    println(packetmeta.type)
    println(packetmeta.salt)

    if(packetmeta.protocolversion != protocolversion || packetmeta.salt != salt) return

    decoder.reset()
    when(state) {
      State.WAITING_CHALLENGE -> {
        when(packetmeta.type) {
          Packet.Type.CHALLENGE.id -> {
            chal.deserialize(decoder)
            salt = salt xor chal.serversalt
            chalre.salt = salt
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
      disconnect.salt = salt
      for (i in 0..10) send(disconnect)
    }
  }
}
