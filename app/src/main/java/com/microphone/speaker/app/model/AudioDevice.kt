package com.microphone.speaker.app.model

data class AudioDevice(
    val id: Int,
    val name: String,
    val type: AudioDeviceType
)

enum class AudioDeviceType {
    MICROPHONE,
    SPEAKER
}

data class AudioUiState(
    val availableMicrophones: List<AudioDevice> = emptyList(),
    val availableSpeakers: List<AudioDevice> = emptyList(),
    val selectedMicrophone: AudioDevice? = null,
    val selectedSpeaker: AudioDevice? = null,
    val isRecording: Boolean = false,
    val errorMessage: String? = null
)