import common.*
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
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

  val clientprivatekey: Long
  val clientpublickey: Long

  var state = State.DISCONNECTED; private set
  var token = 0; private set
  var clientid = 0; private set
  var lastkeepalivetimestamp = 0L; private set

  private val commands = ArrayList<Command>()

  init {
    clientprivatekey = 1
    clientpublickey = 1

    outpacket.socketAddress = InetSocketAddress(localaddress,serverport)
    connreq.clientpublickey = clientpublickey
  }

  private fun connreq() {
    connreq.clientpublickey = clientpublickey
    send(connreq)
    state = State.WAITING_CHALLENGE
  }

  override fun emit() {
    when(state) {
      State.DISCONNECTED -> {
        connreq()
      }
      State.WAITING_CHALLENGE -> {
        send(connreq)
      }
      State.WAITING_ACCEPTED -> {
        send(chalre)
      }
      State.CONNECTED -> {
        if(commands.isNotEmpty())
          send(commands.first())
        if(lastkeepalivetimestamp < emittimestamp) {
          lastkeepalivetimestamp = emittimestamp
          send(keep)
        }
      }
    }
  }

  override fun receive() {
    try {
      socket.receive(inpacket)
    } catch(ste: SocketTimeoutException) {
      println(ste)
      return
    }

    if(badmeta()) return
    super.receive()

    decoder.reset()

    when(state) {
      State.WAITING_CHALLENGE -> {
        when(packetmeta.type) {
          Packet.Type.CHALLENGE.id -> {
            chal.deserialize(decoder)
            chalre.token = chal.token
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
            //
          }
          Packet.Type.COMMAND_ACK.id -> {
            comack.deserialize(decoder)
            commands.removeIf { it.commanddate == comack.commanddate && it.command == comack.command }
          }
        }
      }
      else -> {
        println("received wrong packet: $packetmeta")
      }
    }
  }

  fun command(b: Byte) {
    commands.add(
      Command().apply {
        this.command = b
        this.commanddate = receivetimestamp
      }
    )
  }

  fun disconnect() {
    if(state == State.CONNECTED) {
      disconnect.token = token
      for (i in 0..10) send(disconnect)
      state = State.DISCONNECTED
      stop()
    }
  }
}
