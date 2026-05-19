package com.privacyshield.remote.model

data class RemoteDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val type: DeviceType,
    val isPaired: Boolean = false,
    val customName: String? = null
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() } ?: name

    fun serialize(): String = listOf(
        id, name, host, port.toString(), type.name,
        isPaired.toString(), customName ?: ""
    ).joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "\u001F" // ASCII unit separator — safe for all fields

        fun deserialize(raw: String): RemoteDevice? = try {
            val p = raw.split(SEPARATOR)
            if (p.size < 7) return null
            RemoteDevice(
                id = p[0],
                name = p[1],
                host = p[2],
                port = p[3].toInt(),
                type = DeviceType.valueOf(p[4]),
                isPaired = p[5].toBoolean(),
                customName = p[6].takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            null
        }

        fun manualId(host: String, port: Int) = "manual:$host:$port"
    }
}

enum class DeviceType(val label: String) {
    ANDROID_TV("Android TV / Google TV"),
    FIRE_TV("Amazon Fire TV"),
    GENERIC_TV("Generic Smart TV"),
    IR_ONLY("IR Device")
}
