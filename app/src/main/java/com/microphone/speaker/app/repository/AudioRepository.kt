package com.microphone.speaker.app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import androidx.core.app.ActivityCompat
import com.microphone.speaker.app.model.AudioDevice
import com.microphone.speaker.app.model.AudioDeviceType
import com.microphone.speaker.app.model.AudioQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor() {
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var currentMicrophone: AudioDevice? = null
    private var currentSpeaker: AudioDevice? = null
    private var currentContext: Context? = null
    
    companion object {
        private const val BLUETOOTH_SCO_AUDIO_SOURCE = 6 // MediaRecorder.AudioSource.BLUETOOTH_SCO
        private const val BLUETOOTH_SCO_STREAM = 6 // Custom stream type for Bluetooth
        
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 1
        
        // Performans modu ayarları
        private const val PERFORMANCE_MODE_LOW_LATENCY = AudioManager.MODE_IN_COMMUNICATION
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
            if (isRecording) {
                return@withContext Result.failure(Exception("Ses aktarımı zaten başlatılmış"))
            }
            
            currentContext = context
            currentMicrophone = microphone
            currentSpeaker = speaker
            
            return@withContext setupAudioDevices(context, microphone, speaker, audioQuality)
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
        try {
            if (!isRecording) {
                return@withContext Result.failure(Exception("Ses aktarımı başlatılmamış"))
            }
            
            // Mevcut ses aktarımını durdur
            stopCurrentAudio()
            
            // Yeni cihazları ayarla
            currentMicrophone = microphone
            currentSpeaker = speaker
            
            return@withContext setupAudioDevices(context, microphone, speaker, audioQuality)
        } catch (e: Exception) {
            Result.failure(Exception("Cihaz değiştirme hatası: ${e.message}"))
        }
    }
    
    private suspend fun setupAudioDevices(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice,
        audioQuality: AudioQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Ses yönlendirmesini yap (Bluetooth SCO, Hoparlör vb.)
            setupAudioRouting(audioManager, speaker)
            
            val sampleRate = audioQuality.sampleRate
            
            // AudioRecord için buffer size hesapla
            val recordBufferSize = AudioRecord.getMinBufferSize(
                sampleRate, 
                CHANNEL_CONFIG_IN, 
                AUDIO_FORMAT
            )
            
            if (recordBufferSize == AudioRecord.ERROR || recordBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return@withContext Result.failure(Exception("AudioRecord buffer size hatası"))
            }
            
            val actualRecordBufferSize = recordBufferSize * BUFFER_SIZE_MULTIPLIER
            
            // AudioRecord oluştur
            // Mikrofon kaynağını belirle
            val audioSource = if (microphone.deviceInfo?.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                MediaRecorder.AudioSource.MIC // Dahili mikrofon için MIC kaynağını zorla
            } else {
                MediaRecorder.AudioSource.DEFAULT // Diğerleri için varsayılan (genellikle VOICE_COMMUNICATION)
            }

            audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                actualRecordBufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioRecord başlatılamadı"))
            }
            
            // Mikrofonu yönlendir (API 23+)
            if (microphone.deviceInfo != null) {
                try {
                    val success = audioRecord?.setPreferredDevice(microphone.deviceInfo)
                    if (success != true) {
                        // Başarısız olsa bile devam et, belki varsayılan çalışır
                    }
                } catch (e: Exception) {
                    // Cihaz seçimi hatası, yoksay
                }
            }
            
            // AudioTrack için buffer size hesapla
            val trackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                CHANNEL_CONFIG_OUT,
                AUDIO_FORMAT
            )
            
            if (trackBufferSize == AudioTrack.ERROR || trackBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                return@withContext Result.failure(Exception("AudioTrack buffer size hatası"))
            }
            
            val actualTrackBufferSize = trackBufferSize * BUFFER_SIZE_MULTIPLIER
            
            // AudioTrack oluştur
            audioTrack = createAudioTrack(AUDIO_FORMAT, sampleRate, actualTrackBufferSize)
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioTrack başlatılamadı"))
            }
            
            // Hoparlörü yönlendir (API 23+)
            if (speaker.deviceInfo != null) {
                try {
                    val success = audioTrack?.setPreferredDevice(speaker.deviceInfo)
                    if (success != true) {
                        // Başarısız olsa bile devam et
                    }
                } catch (e: Exception) {
                    // Cihaz seçimi hatası, yoksay
                }
            }
            
            // Kayıt ve çalmayı başlat
            audioRecord?.startRecording()
            audioTrack?.play()
            
            isRecording = true
            
            // Ses aktarım döngüsü - ayrı thread'de çalıştır
            startAudioLoop(actualRecordBufferSize)
            
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Mikrofon izni gerekli: ${e.message}"))
        } catch (e: IllegalStateException) {
            Result.failure(Exception("Audio cihazı kullanımda: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Ses aktarımı başlatılamadı: ${e.message}"))
        }
    }

    private fun setupAudioRouting(audioManager: AudioManager, speaker: AudioDevice) {
        val type = speaker.deviceInfo?.type ?: return

        // Reset states
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION // Low latency mode
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = false

        when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                audioManager.isSpeakerphoneOn = true
            }
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET -> {
                audioManager.isSpeakerphoneOn = false
            }
        }
    }

    private fun createAudioTrack(
        audioFormat: Int,
        sampleRate: Int,
        bufferSize: Int
    ): AudioTrack {
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }
    
    private fun stopCurrentAudio() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }
    
    private suspend fun startAudioLoop(bufferSize: Int) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(bufferSize)
        var consecutiveErrors = 0
        val maxConsecutiveErrors = 5 // 10'dan 5'e düşürüldü
        
        try {
            while (isRecording && consecutiveErrors < maxConsecutiveErrors) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                when {
                    bytesRead > 0 -> {
                        consecutiveErrors = 0 // Reset error counter
                        
                        // Anında yazma - gecikmeyi azaltmak için
                        val bytesWritten = audioTrack?.write(buffer, 0, bytesRead, AudioTrack.WRITE_NON_BLOCKING) ?: 0
                        
                        if (bytesWritten < 0) {
                            consecutiveErrors++
                        }
                    }
                    bytesRead == AudioRecord.ERROR_INVALID_OPERATION -> {
                        consecutiveErrors++
                    }
                    bytesRead == AudioRecord.ERROR_BAD_VALUE -> {
                        consecutiveErrors++
                    }
                    else -> {
                        consecutiveErrors++
                    }
                }
                
                // Sadece hata durumunda bekleme - normal durumda gecikme yok
                if (consecutiveErrors > 0) {
                    kotlinx.coroutines.delay(1)
                }
            }
        } catch (e: Exception) {
            // Ses döngüsü hatası - sessizce durdur
        } finally {
            isRecording = false
        }
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