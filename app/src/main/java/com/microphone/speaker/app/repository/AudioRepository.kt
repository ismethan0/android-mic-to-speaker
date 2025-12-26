package com.microphone.speaker.app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import androidx.core.app.ActivityCompat
import com.microphone.speaker.app.model.AudioDevice
import com.microphone.speaker.app.model.AudioDeviceType
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
    
    fun getAvailableSpeakers(context: Context): List<AudioDevice> {
        val speakers = mutableListOf<AudioDevice>()
        
        // Dahili hoparlör
        speakers.add(
            AudioDevice(
                id = AudioManager.STREAM_MUSIC,
                name = "Dahili Hoparlör",
                type = AudioDeviceType.SPEAKER
            )
        )
        
        // Bluetooth hoparlör kontrolü
        if (hasBluetoothPermission(context)) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            if (audioManager.isBluetoothScoAvailableOffCall) {
                speakers.add(
                    AudioDevice(
                        id = BLUETOOTH_SCO_STREAM,
                        name = "Bluetooth Hoparlör",
                        type = AudioDeviceType.SPEAKER
                    )
                )
            }
        }
        
        return speakers
    }
    
    suspend fun startAudioTransfer(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isRecording) {
                return@withContext Result.failure(Exception("Ses aktarımı zaten başlatılmış"))
            }
            
            currentContext = context
            currentMicrophone = microphone
            currentSpeaker = speaker
            
            return@withContext setupAudioDevices(context, microphone, speaker)
        } catch (e: SecurityException) {
            Result.failure(Exception("Mikrofon izni gerekli: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Ses aktarımı başlatılamadı: ${e.message}"))
        }
    }
    
    suspend fun changeAudioDevices(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice
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
            
            return@withContext setupAudioDevices(context, microphone, speaker)
        } catch (e: Exception) {
            Result.failure(Exception("Cihaz değiştirme hatası: ${e.message}"))
        }
    }
    
    private suspend fun setupAudioDevices(
        context: Context,
        microphone: AudioDevice,
        speaker: AudioDevice
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Önce mevcut Bluetooth SCO'yu kapat
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            
            // Audio routing ayarları
            setupAudioRouting(audioManager, speaker)
            
            // AudioRecord ayarları
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return@withContext Result.failure(Exception("AudioRecord buffer size hatası"))
            }
            
            // AudioRecord oluştur
            audioRecord = AudioRecord(
                microphone.id,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioRecord başlatılamadı"))
            }
            
            // AudioTrack ayarları
            val trackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat
            )
            
            if (trackBufferSize == AudioTrack.ERROR || trackBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                return@withContext Result.failure(Exception("AudioTrack buffer size hatası"))
            }
            
            // AudioTrack oluştur - hoparlör tipine göre
            audioTrack = createAudioTrack(audioFormat, sampleRate, trackBufferSize, speaker)
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioTrack başlatılamadı"))
            }
            
            // Kayıt ve çalmayı başlat
            audioRecord?.startRecording()
            audioTrack?.play()
            
            isRecording = true
            
            // Ses aktarım döngüsü - ayrı thread'de çalıştır
            startAudioLoop(bufferSize)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun setupAudioRouting(audioManager: AudioManager, speaker: AudioDevice) {
        when (speaker.id) {
            BLUETOOTH_SCO_STREAM -> {
                // Bluetooth hoparlör için
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
            AudioManager.STREAM_MUSIC -> {
                // Dahili hoparlör için
                audioManager.isBluetoothScoOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
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
            else -> AudioAttributes.USAGE_MEDIA
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
        try {
            while (isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    audioTrack?.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: Exception) {
            // Ses döngüsü hatası - sessizce durdur
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