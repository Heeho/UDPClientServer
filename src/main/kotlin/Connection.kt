import java.net.InetAddress

class Connection (
    var address: InetAddress,
    var clientpublickey: Long,
    var token: Int,
    var connected: Boolean,
    var lastpackettimestamp: Long
) {
    override fun toString(): String = "$address, $clientpublickey, $token, $connected, $lastpackettimestamp"
}