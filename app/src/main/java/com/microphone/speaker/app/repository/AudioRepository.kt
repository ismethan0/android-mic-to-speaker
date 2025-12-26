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
        
        // Dahili mikrofon
        microphones.add(
            AudioDevice(
                id = MediaRecorder.AudioSource.MIC,
                name = "Dahili Mikrofon",
                type = AudioDeviceType.MICROPHONE
            )
        )
        
        // Bluetooth mikrofon (eğer varsa)
        if (hasBluetoothPermission(context)) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            if (audioManager.isBluetoothScoAvailableOffCall) {
                microphones.add(
                    AudioDevice(
                        id = BLUETOOTH_SCO_AUDIO_SOURCE,
                        name = "Bluetooth Mikrofon",
                        type = AudioDeviceType.MICROPHONE
                    )
                )
            }
        }
        
        return microphones
    }
    
    fun getCurrentSpeaker(context: Context): AudioDevice {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        return when {
            audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn -> {
                AudioDevice(
                    id = BLUETOOTH_SCO_STREAM,
                    name = "Sistem Hoparlörü (Bluetooth)",
                    type = AudioDeviceType.SPEAKER
                )
            }
            else -> {
                AudioDevice(
                    id = AudioManager.STREAM_MUSIC,
                    name = "Sistem Hoparlörü",
                    type = AudioDeviceType.SPEAKER
                )
            }
        }
    }
    
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
            val sampleRate = audioQuality.sampleRate
            
            // Önce mevcut Bluetooth SCO'yu kapat
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            
            // Audio routing ayarları
            setupAudioRouting(audioManager, speaker)
            
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
            audioRecord = AudioRecord(
                microphone.id,
                sampleRate,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                actualRecordBufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioRecord başlatılamadı"))
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
            
            // AudioTrack oluştur - hoparlör tipine göre
            audioTrack = createAudioTrack(AUDIO_FORMAT, sampleRate, actualTrackBufferSize, speaker)
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioTrack başlatılamadı"))
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
        when (speaker.id) {
            BLUETOOTH_SCO_STREAM -> {
                // Bluetooth hoparlör için - düşük gecikme modu
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                audioManager.mode = PERFORMANCE_MODE_LOW_LATENCY
            }
            AudioManager.STREAM_MUSIC -> {
                // Dahili hoparlör için - düşük gecikme modu
                audioManager.isBluetoothScoOn = false
                audioManager.mode = PERFORMANCE_MODE_LOW_LATENCY
                audioManager.isSpeakerphoneOn = true
            }
        }
    }
    
    private fun createAudioTrack(
        audioFormat: Int,
        sampleRate: Int,
        bufferSize: Int,
        speaker: AudioDevice
    ): AudioTrack {
        val usage = when (speaker.id) {
            BLUETOOTH_SCO_STREAM -> AudioAttributes.USAGE_VOICE_COMMUNICATION
            else -> AudioAttributes.USAGE_VOICE_COMMUNICATION // USAGE_MEDIA'dan değiştirildi
        }
        
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
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
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) // Düşük gecikme modu
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