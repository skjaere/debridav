package io.william.debridav.debrid.realdebrid

class MagnetParser {
    companion object {
        fun getHashFromMagnet(magnet: String): String? {
            val params = magnet
                    .replace("magnet:", "")
                    .replace("?", "")
                    .split("&")
                    .associate {
                        val pair = it.split("=")
                        pair[0] to pair[1]
                    }
            return params["xt"]?.replace("urn:btih:", "")
        }
    }
}