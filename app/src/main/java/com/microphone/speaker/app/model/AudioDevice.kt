package com.microphone.speaker.app.model

import android.media.AudioDeviceInfo
import androidx.annotation.StringRes
import com.microphone.speaker.app.R

data class AudioDevice(
    val id: Int,
    val name: String,
    val type: AudioDeviceType,
    val deviceInfo: AudioDeviceInfo? = null
)

enum class AudioDeviceType {
    MICROPHONE,
    SPEAKER
}

enum class AudioQuality(
    @StringRes val displayNameRes: Int,
    val sampleRate: Int,
    @StringRes val descriptionRes: Int
) {
    LOW_LATENCY(R.string.quality_low_latency, 16000, R.string.quality_low_latency_desc),
    BALANCED(R.string.quality_balanced, 22050, R.string.quality_balanced_desc),
    HIGH_QUALITY(R.string.quality_high_quality, 44100, R.string.quality_high_quality_desc)
}

data class AudioUiState(
    val availableMicrophones: List<AudioDevice> = emptyList(),
    val availableSpeakers: List<AudioDevice> = emptyList(),
    val selectedMicrophone: AudioDevice? = null,
    val selectedSpeaker: AudioDevice? = null,
    val selectedAudioQuality: AudioQuality = AudioQuality.BALANCED,
    val isRecording: Boolean = false,
    val errorMessage: String? = null
)