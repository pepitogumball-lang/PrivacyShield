package com.privacyshield.remote.model

/**
 * Commands map to Android KeyEvent key codes.
 * These are the same codes used by the Android TV Remote protocol.
 */
enum class RemoteCommand(val keyCode: Int, val label: String) {
    DPAD_UP(19, "Up"),
    DPAD_DOWN(20, "Down"),
    DPAD_LEFT(21, "Left"),
    DPAD_RIGHT(22, "Right"),
    DPAD_CENTER(23, "OK"),
    BACK(4, "Back"),
    HOME(3, "Home"),
    MENU(82, "Menu"),
    MEDIA_PLAY_PAUSE(85, "Play / Pause"),
    MEDIA_PLAY(126, "Play"),
    MEDIA_PAUSE(127, "Pause"),
    MEDIA_REWIND(89, "Rewind"),
    MEDIA_FAST_FORWARD(90, "Fast Forward"),
    VOLUME_UP(24, "Volume Up"),
    VOLUME_DOWN(25, "Volume Down"),
    VOLUME_MUTE(164, "Mute"),
    POWER(26, "Power"),
}
