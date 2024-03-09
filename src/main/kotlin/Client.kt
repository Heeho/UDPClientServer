import java.net.DatagramPacket
import java.net.DatagramSocket

class Client: Application extends EmitterReceiver {
  private enum class States {
    DISCONNECTED,
    WAITING_CHALLENGE,
    WAITING_ACCEPTED,
    CONNECTED
  }

  private var state = States.DISCONNECTED
  private var salt = 0
  private var clientid = 0
  private val gamestate = GameState()
  private var timestamp = 0
  private val commands = Queue<Command>()

  fun command(c: Command) {
    commands.enq(c)
  }

  override fun run() {
    timestamp = System.currentTimeMillis/1000
    emit()
    receive()
    purge()
  }

  override private fun emit() {
    when(state) {
      States.DISCONNECTED -> {
        salt = newsalt()
        connreq.salt = salt
        send(connreq)
        state = States.WAITING_CHALLENGE
      }
      States.WAITING_CHALLENGE -> {
        send(connreq)
      }
      States.WAITING_ACCEPTED -> {
        send(chalre)
      }
      States.CONNECTED -> {
        //if(!sendcontrol())
        send(keep)
      }
    }
  }

  override private fun purge() {
    for(c in commands) {
      if(timestamp - c.commanddate > commandtimeout) {
        commands.remove(c)
      }
    }
  }

  override private fun receive() {
    if(state == States.DISCONNECTED) return

    try {
      insocket.receive(inpacket)
    } catch(e: Exception) {
      if(state == States.CONNECTED) {
        state = States.DISCONNECTED
      }
      return
    }

    decoder.reset()
    packetmeta.deserialize(decoder)
    if(packetmeta.protocolversion != protocolversion || packetmeta.salt != salt) return

    decoder.reset()
    when(state) {
      States.WAITING_CHALLENGE -> {
        when(packetmeta.type) {
          Packet.Type.CHALLENGE.id -> {
            chal.deserialize(decoder)
            salt = salt xor chal.serversalt
            chalre.salt = salt
            send(chalre)
            state = States.WAITING_ACCEPTED
          }
        }
      }
      States.WAITING_ACCEPTED -> {
        when(packetmeta.type) {
          Packet.Type.CONNECTION_ACCEPTED.id -> {
            connacc.deserialize(decoder)
            clientid = connacc.clientid
            state = States.CONNECTED
          }
          Packet.Type.CONNECTION_REFUSED.id -> {
            state = States.DISCONNECTED
          }
        }
      }
      States.CONNECTED -> {
        when(packetmeta.type) {
          Packet.Type.SERVER_STATE.id -> {
            gamestate.deserialize(decoder)
          }
          Packets.CONTROL_ACK.id -> {
            controlack.deserialize(decoder)
            val c = commands.peek()
            if(c.commanddate == controlack.commanddate && c.command == controlack.command)
              commands.poll()
          }
        }
      }
      else -> {}
    }
  }
}
