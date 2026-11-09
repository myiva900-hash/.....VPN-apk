package com.example.model

data class VpnServer(
    val id: String,
    val name: String,
    val location: String,
    val countryCode: String,
    val flag: String,
    val ip: String,
    val pingMs: Int,
    val supportedPorts: List<Int> = listOf(80, 443, 8080, 8888, 3128, 53),
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_SERVERS = listOf(
            VpnServer(
                id = "rw_gisozi",
                name = "Gisozi VIP Node 01",
                location = "Gisozi Kigali, Rwanda",
                countryCode = "RW",
                flag = "🇷🇼",
                ip = "197.243.16.2",
                pingMs = 12
            ),
            VpnServer(
                id = "rw_remera",
                name = "Remera Ultra Fast",
                location = "Remera Kigali, Rwanda",
                countryCode = "RW",
                flag = "🇷🇼",
                ip = "197.243.16.18",
                pingMs = 15
            ),
            VpnServer(
                id = "rw_kacyiru",
                name = "Kacyiru Premium Relay",
                location = "Kigali, Rwanda",
                countryCode = "RW",
                flag = "🇷🇼",
                ip = "197.243.16.34",
                pingMs = 14
            ),
            VpnServer(
                id = "random",
                name = "Random Server",
                location = "Any Fast Location",
                countryCode = "AUTO",
                flag = "🌐",
                ip = "198.51.100.1",
                pingMs = 28
            ),
            VpnServer(
                id = "us_east",
                name = "United States - East",
                location = "New York",
                countryCode = "US",
                flag = "🇺🇸",
                ip = "198.51.100.12",
                pingMs = 52
            ),
            VpnServer(
                id = "de_fra",
                name = "Germany - Frankfurt",
                location = "Frankfurt",
                countryCode = "DE",
                flag = "🇩🇪",
                ip = "198.51.100.24",
                pingMs = 38
            ),
            VpnServer(
                id = "sg_sin",
                name = "Singapore - Central",
                location = "Singapore",
                countryCode = "SG",
                flag = "🇸🇬",
                ip = "198.51.100.35",
                pingMs = 64
            ),
            VpnServer(
                id = "uk_lon",
                name = "United Kingdom",
                location = "London",
                countryCode = "GB",
                flag = "🇬🇧",
                ip = "198.51.100.41",
                pingMs = 42
            ),
            VpnServer(
                id = "fr_par",
                name = "France - Paris",
                location = "Paris",
                countryCode = "FR",
                flag = "🇫🇷",
                ip = "198.51.100.67",
                pingMs = 46
            )
        )
    }
}
