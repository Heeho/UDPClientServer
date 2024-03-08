import java.io.DataInputStream
import java.io.DataOutputStream

abstract class Packet {
    abstract fun serialize(d :DataOutputStream)
    //abstract fun deserialize(d : DataInputStream)
}

class ConnectionRequest(
    val clientsalt: Int = 0
): Packet() {
    override fun serialize(d: DataOutputStream) {
        d.writeInt(Common.version)
        d.writeByte(Common.CONNECTION_REQUEST)
        d.writeInt(clientsalt)
    }
}