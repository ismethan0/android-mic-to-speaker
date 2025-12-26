package com.microphone.speaker.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.microphone.speaker.app.viewmodel.AudioViewModel

@Composable
fun MicrophoneSpeakerApp(
    viewModel: AudioViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.initializeAudio(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mikrofon → Hoparlör",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Mikrofon Seçimi
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Mikrofon Seçimi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                uiState.availableMicrophones.forEach { microphone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (microphone.id == uiState.selectedMicrophone?.id),
                                onClick = { viewModel.selectMicrophone(microphone) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (microphone.id == uiState.selectedMicrophone?.id),
                            onClick = { viewModel.selectMicrophone(microphone) }
                        )
                        Text(
                            text = microphone.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Hoparlör Seçimi
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Hoparlör Seçimi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                uiState.availableSpeakers.forEach { speaker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (speaker.id == uiState.selectedSpeaker?.id),
                                onClick = { viewModel.selectSpeaker(speaker) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (speaker.id == uiState.selectedSpeaker?.id),
                            onClick = { viewModel.selectSpeaker(speaker) }
                        )
                        Text(
                            text = speaker.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Başlat/Durdur Butonu
        Button(
            onClick = {
                if (uiState.isRecording) {
                    viewModel.stopAudioTransfer()
                } else {
                    viewModel.startAudioTransfer()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState.selectedMicrophone != null && uiState.selectedSpeaker != null
        ) {
            Text(
                text = if (uiState.isRecording) "Durdur" else "Başlat",
                fontSize = 18.sp
            )
        }

        // Durum Göstergesi
        if (uiState.isRecording) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "🎤 Aktif - Mikrofon sesi hoparlöre aktarılıyor",
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "💡 Cihazları değiştirebilirsiniz",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Hata Mesajı
        uiState.errorMessage?.let { error ->
            Text(
                text = "Hata: $error",
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}