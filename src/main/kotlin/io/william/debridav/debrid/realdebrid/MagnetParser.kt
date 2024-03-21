package io.william.debridav.debrid.realdebrid

class MagnetParser {
    companion object {
        fun getHashFromMagnet(magnet: String): String? {
            //"magnet:?xt=urn:btih:T3GUM5X5B4CHIFI2JN2KLFMPIJRZZ267&dn=ubuntu-23.10.1-desktop-amd64.iso&xl=5173995520&tr.1=https%3A%2F%2Ftorrent.ubuntu.com%2Fannounce&tr.2=https%3A%2F%2Ftorrent.ubuntu.com%2Fannounce&tr.3=https%3A%2F%2Fipv6.torrent.ubuntu.com%2Fannounce"
            val params = magnet
                    .replace("magnet:", "")
                    .replace("?", "")
                    .split("&")
                    .associate {
                        val pair = it.split("=")
                        Pair(pair[0], pair[1])
                    }
            return params["xt"]?.replace("urn:btih:", "")
        }
    }
}