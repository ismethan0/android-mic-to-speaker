package com.microphone.speaker.app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import com.microphone.speaker.app.model.AudioDevice
import com.microphone.speaker.app.model.AudioDeviceType
import com.microphone.speaker.app.model.AudioQuality
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AudioRepository @Inject constructor() {
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var loopJob: Job? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentMicrophone: AudioDevice? = null
    private var currentSpeaker: AudioDevice? = null
    private var currentContext: Context? = null
    
    companion object {
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    fun getAvailableMicrophones(context: Context): List<AudioDevice> {
        val microphones = mutableListOf<AudioDevice>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        
        for (device in devices) {
            // Sadece kaynak (giriş) olabilen cihazları al
            if (!device.isSource) continue

            val name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                device.address.takeIf { it.isNotEmpty() } ?: device.productName.toString()
            } else {
                device.productName.toString()
            }
            
            microphones.add(
                AudioDevice(
                    id = device.id,
                    name = "${getDeviceTypeName(device.type, AudioDeviceType.MICROPHONE)} ($name)",
                    type = AudioDeviceType.MICROPHONE,
                    deviceInfo = device,
                    icon = com.microphone.speaker.app.R.drawable.ic_microphone_small
                )
            )
        }
        
        return microphones
    }
    
    fun getAvailableSpeakers(context: Context): List<AudioDevice> {
        val speakers = mutableListOf<AudioDevice>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        for (device in devices) {
            // Sadece alıcı (çıkış) olabilen cihazları al
            if (!device.isSink) continue

            val name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                device.address.takeIf { it.isNotEmpty() } ?: device.productName.toString()
            } else {
                device.productName.toString()
            }
            
            speakers.add(
                AudioDevice(
                    id = device.id,
                    name = "${getDeviceTypeName(device.type, AudioDeviceType.SPEAKER)} ($name)",
                    type = AudioDeviceType.SPEAKER,
                    deviceInfo = device,
                    icon = com.microphone.speaker.app.R.drawable.ic_speaker
                )
            )
        }
        
        return speakers
    }

    private fun getDeviceTypeName(type: Int, deviceType: AudioDeviceType): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Dahili Mikrofon"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> if (deviceType == AudioDeviceType.MICROPHONE) "Bluetooth Mikrofon" else "Bluetooth Kulaklık"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Hoparlör"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Dahili Hoparlör"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> if (deviceType == AudioDeviceType.MICROPHONE) "Kablolu Kulaklık Mikrofonu" else "Kablolu Kulaklık"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Kablolu Kulaklık"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Cihaz"
            AudioDeviceInfo.TYPE_USB_HEADSET -> if (deviceType == AudioDeviceType.MICROPHONE) "USB Kulaklık Mikrofonu" else "USB Kulaklık"
            else -> "Ses Cihazı"
        }
    }
    
    // getCurrentSpeaker kaldırıldı, yerine getAvailableSpeakers kullanılacak
    
    suspend fun startAudioTransfer(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice,
        audioQuality: AudioQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            currentContext = context
            currentMicrophone = microphone
            currentSpeaker = speaker

            if (isRecording) {
                stopCurrentAudio()
            }

            return@withContext prepareAndStart(context, microphone, speaker, audioQuality)
        } catch (e: SecurityException) {
            Result.failure(Exception("Mikrofon izni gerekli: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Ses aktarımı başlatılamadı: ${e.message}"))
        }
    }

    suspend fun changeAudioDevices(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice,
        audioQuality: AudioQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        currentContext = context
        currentMicrophone = microphone
        currentSpeaker = speaker

        if (!isRecording) {
            return@withContext prepareAndStart(context, microphone, speaker, audioQuality)
        }

        stopCurrentAudio()
        return@withContext prepareAndStart(context, microphone, speaker, audioQuality)
    }
    
    private fun prepareAndStart(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice,
        audioQuality: AudioQuality
    ): Result<Unit> {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (speaker.deviceInfo?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && !hasBluetoothPermission(context)) {
                return Result.failure(Exception("Bluetooth izni gerekli"))
            }

            handleBluetoothRouting(audioManager, speaker)

            val sampleRate = audioQuality.sampleRate

            val recordBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
            if (recordBufferSize == AudioRecord.ERROR || recordBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return Result.failure(Exception("AudioRecord buffer size hatası"))
            }

            val trackBufferSize = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
            if (trackBufferSize == AudioTrack.ERROR || trackBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                return Result.failure(Exception("AudioTrack buffer size hatası"))
            }

            val preferredMicSource = if (microphone.deviceInfo?.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                MediaRecorder.AudioSource.MIC
            } else {
                MediaRecorder.AudioSource.DEFAULT
            }

            val newAudioRecord = AudioRecord.Builder()
                .setAudioSource(preferredMicSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG_IN)
                        .build()
                )
                .setBufferSizeInBytes(recordBufferSize)
                .build()

            if (newAudioRecord.state != AudioRecord.STATE_INITIALIZED) {
                newAudioRecord.release()
                return Result.failure(Exception("AudioRecord başlatılamadı"))
            }

            microphone.deviceInfo?.let { deviceInfo ->
                runCatching { newAudioRecord.setPreferredDevice(deviceInfo) }
            }

            val newAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .build()

            if (newAudioTrack.state != AudioTrack.STATE_INITIALIZED) {
                newAudioTrack.release()
                newAudioRecord.release()
                return Result.failure(Exception("AudioTrack başlatılamadı"))
            }

            speaker.deviceInfo?.let { deviceInfo ->
                runCatching { newAudioTrack.setPreferredDevice(deviceInfo) }
            }

            stopCurrentAudio()

            audioRecord = newAudioRecord
            audioTrack = newAudioTrack

            audioRecord?.startRecording()
            audioTrack?.play()

            isRecording = true

            // Daha küçük buffer seçerek gecikmeyi azalt
            val loopBufferSize = min(recordBufferSize, trackBufferSize)
            startAudioLoop(loopBufferSize)

            return Result.success(Unit)
        } catch (e: SecurityException) {
            return Result.failure(Exception("Mikrofon izni gerekli: ${e.message}"))
        } catch (e: IllegalStateException) {
            return Result.failure(Exception("Audio cihazı kullanımda: ${e.message}"))
        } catch (e: Exception) {
            return Result.failure(Exception("Ses aktarımı başlatılamadı: ${e.message}"))
        }
    }

    private fun handleBluetoothRouting(audioManager: AudioManager, speaker: AudioDevice) {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = false

        when (speaker.deviceInfo?.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                audioManager.isSpeakerphoneOn = true
            }
        }
    }

    private fun startAudioLoop(bufferSize: Int) {
        loopJob?.cancel()
        val buffer = ByteArray(bufferSize)

        loopJob = ioScope.launch {
            while (isRecording && isActive) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (bytesRead > 0) {
                    audioTrack?.write(buffer, 0, bytesRead, AudioTrack.WRITE_BLOCKING)
                }
            }
            isRecording = false
        }
    }

    private fun stopCurrentAudio() {
        isRecording = false
        loopJob?.cancel()
        loopJob = null

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
                release()
            }
        } catch (_: Exception) {
        }
        audioRecord = null

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) stop()
                release()
            }
        } catch (_: Exception) {
        }
        audioTrack = null
    }
    
    fun stopAudioTransfer(context: Context) {
        isRecording = false
        
        stopCurrentAudio()
        
        // Audio routing'i sıfırla
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
        } catch (e: Exception) {
            // Audio manager hatası - devam et
        }
        
        currentContext = null
        currentMicrophone = null
        currentSpeaker = null
    }
    
    private fun hasBluetoothPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}