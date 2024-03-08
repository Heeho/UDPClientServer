import java.net.InetAddress

class Common {
    companion object {
        val serveraddress = InetAddress.getByName("127.0.0.1")
        val version = "1.0.0".hashCode()

        const val serverport = 40708
        const val clientPacketPadding = 100
        const val maxPacketSize = 100

        const val DISCONNECT = 0
        const val CONNECTION_REQUEST = 1
        const val CHALLENGE = 2
        const val CHALLENGE_RESPONSE = 3
        const val CONNECTION_ACCEPTED = 4
        const val CLIENT_PAYLOAD = 5
        const val SERVER_PAYLOAD = 6

        val CLIENT_PADDED_CONNECTION_PACKETS = listOf(CONNECTION_REQUEST, CHALLENGE_RESPONSE)
    }
}

enum class Packets {
  CONNECTION_REQUEST(1),
  CHALLENGE(0),
  CHALLENGE_RESPONSE(2),
  CONNECTION_ACCEPTED(-1),
  CONNECTION_REFUSED(-2),
  GAME_STATE(-3),
  PLAYER_CONTROL(3),
  KEEP_ALIVE(4)

  val type: Byte
}


class GameState {
  val players: Array<Player>(max)
  var id = IntArray(max)
  var hp = IntArray(max)
  ...

  fun serialize(e: Encoder) = e.write(id).write(hd)...

  fun deserialize(d: Decoder) {
    id = d.readInt()
    ...
  }
}
