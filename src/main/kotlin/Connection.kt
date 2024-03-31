import java.net.InetSocketAddress

class Connection (
) {
    enum class State {
        DISCONNECTED,
        WAITING_CHALLENGE,
        WAITING_ACCEPTED,
        CONNECTED
    }

    var state: State = State.DISCONNECTED
    var receiveraddress: InetSocketAddress? = null
    var receiverpublickey: Long? = null
    var token: Int? = null
    var lastreceivetimestamp: Long? = null
    var lastsendtimestamp: Long? = null

    override fun toString(): String = "$receiveraddress, $receiverpublickey, $token, ${state.name}, $lastreceivetimestamp, $lastsendtimestamp"
}