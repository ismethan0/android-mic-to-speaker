package com.microphone.speaker.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microphone.speaker.app.model.AudioDevice
import com.microphone.speaker.app.model.AudioUiState
import com.microphone.speaker.app.model.AudioQuality
import com.microphone.speaker.app.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()
    
    private var context: Context? = null
    
    fun initializeAudio(context: Context) {
        this.context = context
        loadAudioDevices()
    }
    
    private fun loadAudioDevices() {
        context?.let { ctx ->
            val microphones = audioRepository.getAvailableMicrophones(ctx)
            val speakers = audioRepository.getAvailableSpeakers(ctx)
            
            _uiState.value = _uiState.value.copy(
                availableMicrophones = microphones,
                availableSpeakers = speakers,
                selectedMicrophone = microphones.firstOrNull(),
                selectedSpeaker = speakers.firstOrNull()
            )
        }
    }
    
    fun selectMicrophone(microphone: AudioDevice) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedMicrophone = microphone,
            errorMessage = null
        )
        
        // Eğer kayıt yapılıyorsa, cihazı değiştir
        if (currentState.isRecording) {
            changeAudioDevices()
        }
    }
    
    fun selectSpeaker(speaker: AudioDevice) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedSpeaker = speaker,
            errorMessage = null
        )
        
        // Eğer kayıt yapılıyorsa, cihazı değiştir
        if (currentState.isRecording) {
            changeAudioDevices()
        }
    }
    
    fun selectAudioQuality(audioQuality: AudioQuality) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedAudioQuality = audioQuality,
            errorMessage = null
        )
        
        // Eğer kayıt yapılıyorsa, kaliteyi değiştir
        if (currentState.isRecording) {
            changeAudioDevices()
        }
    }
    
    private fun changeAudioDevices() {
        val currentState = _uiState.value
        val microphone = currentState.selectedMicrophone
        val speaker = currentState.selectedSpeaker
        val audioQuality = currentState.selectedAudioQuality
        val ctx = context
        
        if (microphone == null || speaker == null || ctx == null) {
            return
        }
        
        viewModelScope.launch {
            audioRepository.changeAudioDevices(ctx, microphone, speaker, audioQuality)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun startAudioTransfer() {
        val currentState = _uiState.value
        val microphone = currentState.selectedMicrophone
        val speaker = currentState.selectedSpeaker
        val audioQuality = currentState.selectedAudioQuality
        val ctx = context
        
        if (microphone == null || speaker == null || ctx == null) {
            _uiState.value = currentState.copy(
                errorMessage = "Mikrofon ve hoparlör seçilmelidir"
            )
            return
        }
        
        if (currentState.isRecording) {
            return // Zaten kayıt yapılıyor
        }
        
        _uiState.value = currentState.copy(
            isRecording = true,
            errorMessage = null
        )
        
        viewModelScope.launch {
            audioRepository.startAudioTransfer(ctx, microphone, speaker, audioQuality)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun stopAudioTransfer() {
        context?.let { ctx ->
            audioRepository.stopAudioTransfer(ctx)
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                errorMessage = null
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        context?.let { ctx ->
            audioRepository.stopAudioTransfer(ctx)
        }
    }
}