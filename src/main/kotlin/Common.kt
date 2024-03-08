import java.net.InetAddress
import java.util.*

class Params {
    companion object {
        val serveraddress = InetAddress.getByName("127.0.0.1")
        val port = 40708
        val protocolversion = "1.0.0".hashCode()
        val sotimeout = 5000
        val lastpackettimeout = 100
        val padding = 1000
        val maxplayers = 11
    }
}

class Utils {
    companion object {
        fun newsalt() = UUID.randomUUID().hashCode()
    }
}

enum class Packets(val id: Byte) {
  CONNECTION_REQUEST(1),
  CHALLENGE(0),
  CHALLENGE_RESPONSE(2),
  CONNECTION_ACCEPTED(-1),
  CONNECTION_REFUSED(-2),
  GAME_STATE(-3),
  PLAYER_CONTROL(3),
  KEEP_ALIVE(4)
}

class GameState {
    //var id = IntArray(11)
    //var hp = IntArray(11)
    //...

    fun serialize(e: Encoder) {
        //= e.write(id).write(hd)...
    }

    fun deserialize(d: Decoder) {
        //id = d.readInt()
        //...
    }
}