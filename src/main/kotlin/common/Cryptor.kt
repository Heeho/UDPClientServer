package common

class Cryptor {
    val privatekey = 1L
    val publickey = 1L

    fun encrypt(b: ByteArray, encryptionkey: Long) {
        //b = f(b,encryptionkey)
    }

    fun sign(b: ByteArray) {
        //b = f(b,privatekey)
    }
}