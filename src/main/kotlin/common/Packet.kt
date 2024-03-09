package common

open class Packet {
    enum class Type(val id: Byte) {
        CONNECTION_REQUEST(1),
        CHALLENGE(-1),
        CHALLENGE_RESPONSE(2),
        CONNECTION_ACCEPTED(-21),
        CONNECTION_REFUSED(-22),

        SERVER_STATE(-3),
        CLIENT_COMMAND(3),
        CLIENT_COMMAND_ACK(-4),
        KEEP_ALIVE(4),

        DISCONNECT(5)
    }

    var protocolversion = Application.protocolversion
    var type: Byte = 0

    open fun serialize(e: Encoder): Encoder = e.write(protocolversion).write(type)

    open fun deserialize(d: Decoder) {
        protocolversion = d.readInt()
        type = d.readByte()
    }
}

open class SaltedPacket: Packet() {
    var salt = 0

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(salt)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        salt = d.readInt()
    }
}

class ConnectionRequest: SaltedPacket() {
    init { type = Type.CONNECTION_REQUEST.id }
}

class Challenge: SaltedPacket() {
    init { type = Type.CHALLENGE.id }

    var serversalt = 0

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(serversalt)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        serversalt = d.readInt()
    }
}

class ChallengeResponse: SaltedPacket() {
    init { type = Type.CHALLENGE_RESPONSE.id }
}

class ConnectionAccepted: SaltedPacket() {
    init { type = Type.CONNECTION_ACCEPTED.id }

    var clientid = 0

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(clientid)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        clientid = d.readInt()
    }
}

class ConnectionRefused: SaltedPacket() {
    init { type = Type.CONNECTION_REFUSED.id }
}

class KeepAlive: SaltedPacket() {
    init { type = Type.KEEP_ALIVE.id }
}

class ServerState: SaltedPacket() {
    init { type = Type.SERVER_STATE.id }

    var data: Byte = 0 //test

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(data)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        data = d.readByte()
    }
}

open class ClientCommand: SaltedPacket() {
    init { type = Type.CLIENT_COMMAND.id }

    var commanddate = 0
    var command: Byte = 0

    override fun serialize(e: Encoder): Encoder = super.serialize(e).write(commanddate).write(command)

    override fun deserialize(d: Decoder) {
        super.deserialize(d)
        commanddate = d.readInt()
        command = d.readByte()
    }
}

class ClientCommandAck: ClientCommand() {
    init { type = Type.CLIENT_COMMAND_ACK.id }
}