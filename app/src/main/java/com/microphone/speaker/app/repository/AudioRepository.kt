package com.microphone.speaker.app.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter?.isEnabled == true) {
                microphones.add(
                    AudioDevice(
                        id = MediaRecorder.AudioSource.BLUETOOTH_SCO,
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
                        id = AudioManager.STREAM_BLUETOOTH_SCO,
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
            
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Bluetooth hoparlör seçildiyse
            if (speaker.id == AudioManager.STREAM_BLUETOOTH_SCO) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
            
            // AudioRecord ayarları
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            // AudioRecord oluştur
            audioRecord = AudioRecord(
                microphone.id,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            // AudioTrack ayarları
            val trackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat
            )
            
            // AudioTrack oluştur
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .build()
            
            // Kayıt ve çalmayı başlat
            audioRecord?.startRecording()
            audioTrack?.play()
            
            isRecording = true
            
            // Ses aktarım döngüsü
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    audioTrack?.write(buffer, 0, bytesRead)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun stopAudioTransfer(context: Context) {
        isRecording = false
        
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
        
        // Bluetooth SCO'yu kapat
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.isBluetoothScoOn) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }
    }
    
    private fun hasBluetoothPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}