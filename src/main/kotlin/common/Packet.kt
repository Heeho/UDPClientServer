package common

open class Packet {
    enum class Type(val id: Byte) {
        CONNECTION_REQUEST(1),
        CHALLENGE(-1),
        CHALLENGE_RESPONSE(2),
        CONNECTION_ACCEPTED(-21),
        CONNECTION_REFUSED(-22),

        SERVER_STATE(-3),
        COMMAND(3),
        COMMAND_ACK(-4),
        KEEP_ALIVE(4),

        DISCONNECT(5)
    }

    var protocolversion = Application.protocolversion
    var type: Byte = 0
    var requiresserverresponse = false

    open fun serialize(e: Encoder): Encoder = e.write(protocolversion).write(type)

    open fun deserialize(d: Decoder) {
        protocolversion = d.readInt()
        type = d.readByte()
    }

    override fun toString(): String = "$protocolversion, $type"
}

class ConnectionRequest(
    var clientpublickey: Long = 0
): Packet() {
    init {
        type = Type.CONNECTION_REQUEST.id
        requiresserverresponse = true
    }

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(clientpublickey)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        clientpublickey = d.readLong()
    }
}

open class TokenPacket: Packet() {
    var token = 0

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(token)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        token = d.readInt()
    }

    override fun toString(): String = super.toString()+", $token"
}

class Challenge: TokenPacket() {
    init {
        type = Type.CHALLENGE.id
    }

    var serverpublickey = 0L

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(serverpublickey)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        serverpublickey = d.readLong()
    }

    override fun toString(): String = super.toString()+", $serverpublickey"
}

class ChallengeResponse: TokenPacket() {
    init {
        type = Type.CHALLENGE_RESPONSE.id
        requiresserverresponse = true
    }
}

class ConnectionAccepted: TokenPacket() {
    init {
        type = Type.CONNECTION_ACCEPTED.id
    }
}

class ConnectionRefused: TokenPacket() {
    init {
        type = Type.CONNECTION_REFUSED.id
    }
}

class ServerState: TokenPacket() {
    init {
        type = Type.SERVER_STATE.id
    }

    var timestamp = 0L //test
    var data: Byte = 0 //test

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(timestamp).write(data)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        timestamp = d.readLong()
        data = d.readByte()
    }

    override fun toString(): String = super.toString()+", $data"
}

class KeepAlive: TokenPacket() {
    init {
        type = Type.KEEP_ALIVE.id
    }
}

open class Command: TokenPacket() {
    init {
        type = Type.COMMAND.id
        requiresserverresponse = true
    }

    var timestamp = 0L
    var id: Byte = 0

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(timestamp).write(id)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        timestamp = d.readLong()
        id = d.readByte()
    }

    override fun toString(): String = super.toString()+", $timestamp, $id"
}

class CommandAck: Command() {
    init {
        type = Type.COMMAND_ACK.id
    }
}

class Disconnect: TokenPacket() {
    init {
        type = Type.DISCONNECT.id
    }
}