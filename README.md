# Mikrofon Hoparlör Uygulaması / Microphone Speaker App

![Build Status](https://github.com/[KULLANICI_ADI]/[REPO_ADI]/workflows/Build%20APK/badge.svg)

Mikrofondan gelen sesi gerçek zamanlı olarak hoparlöre aktaran basit Android uygulaması.

A simple Android app that transfers microphone audio to speakers in real-time.

## 🎤 Özellikler / Features

**Türkçe:**
- **Gerçek Zamanlı Ses Aktarımı**: Mikrofondan gelen ses anında hoparlöre aktarılır
- **Çoklu Cihaz Desteği**: 
  - Dahili mikrofon
  - Bluetooth mikrofon
  - Dahili hoparlör  
  - Bluetooth hoparlör
- **Canlı Cihaz Değiştirme**: Ses aktarımı sırasında cihaz değiştirebilirsiniz
- **Kolay Kullanım**: Basit ve sezgisel arayüz
- **Modern Tasarım**: Jetpack Compose ile oluşturulmuş modern UI

**English:**
- **Real-Time Audio Transfer**: Microphone audio is instantly transferred to speakers
- **Multi-Device Support**: 
  - Internal microphone
  - Bluetooth microphone
  - Internal speaker  
  - Bluetooth speaker
- **Live Device Switching**: Change devices during audio transfer
- **Easy to Use**: Simple and intuitive interface
- **Modern Design**: Modern UI built with Jetpack Compose

## 📱 Ekran Görüntüleri / Screenshots

<img src="screenshots/phone_my_garden.png" width="300"/>

## 📥 İndirme / Download

### GitHub Releases (Önerilen / Recommended)

**Türkçe:**
1. [Releases](../../releases) sayfasından en son sürümü indirin
2. **⚠️ ÖNEMLİ**: **Debug APK** kullanın (kolay kurulum, imza sorunu yok)
3. Release APK imzasız olduğu için kurulum sorunları yaşayabilirsiniz
4. Android cihazınızda APK'yı yükleyin

**English:**
1. Download the latest version from [Releases](../../releases) page
2. **⚠️ IMPORTANT**: Use **Debug APK** (easy installation, no signature issues)
3. Release APK may have installation issues due to lack of signature
4. Install the APK on your Android device

### APK Seçenekleri / APK Options

| APK Türü / Type | Dosya Adı / Filename | Önerilen / Recommended | Açıklama / Description |
|------------------|---------------------|----------------------|----------------------|
| **Debug APK** ✅ | `microphone-speaker-v1.0.0-debug.apk` | **EVET / YES** | Debug key ile imzalı, kolay kurulum / Signed with debug key, easy installation |
| Release APK | `microphone-speaker-v1.0.0-release-unsigned.apk` | Hayır / No | İmzasız, kurulum sorunları olabilir / Unsigned, may have installation issues |

### Manuel Build / Manual Build
```bash
git clone https://github.com/[KULLANICI_ADI]/[REPO_ADI].git
cd [REPO_ADI]
./gradlew assembleDebug  # Debug APK için / For Debug APK
./gradlew assembleRelease  # Release APK için / For Release APK
```

## 🔧 Gereksinimler / Requirements

**Türkçe:**
- **Android 7.0** (API 24) ve üzeri
- **Mikrofon erişim izni**
- **Ses ayarları değiştirme izni**
- **Bluetooth erişim izni** (Bluetooth cihazlar için)

**English:**
- **Android 7.0** (API 24) and above
- **Microphone access permission**
- **Audio settings modification permission**
- **Bluetooth access permission** (for Bluetooth devices)

## 📋 İzinler / Permissions

Uygulama aşağıdaki izinleri gerektirir / The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## 🛠️ Teknolojiler / Technologies

- **Kotlin** - Ana programlama dili / Main programming language
- **Jetpack Compose** - Modern UI toolkit
- **Hilt** - Dependency injection
- **AudioRecord/AudioTrack** - Ses kayıt ve çalma / Audio recording and playback
- **MVVM Architecture** - Mimari deseni / Architecture pattern

## 📖 Kullanım / Usage

**Türkçe:**
1. Uygulamayı açın
2. Kullanmak istediğiniz mikrofonu seçin
3. Ses çıkışı için hoparlörü seçin
4. "Başlat" butonuna basın
5. Mikrofondan gelen ses hoparlöre aktarılmaya başlar
6. **💡 İpucu**: Ses aktarımı sırasında cihazları değiştirebilirsiniz
7. "Durdur" butonuna basarak aktarımı sonlandırın

**English:**
1. Open the app
2. Select the microphone you want to use
3. Select the speaker for audio output
4. Press the "Start" button
5. Microphone audio starts transferring to the speaker
6. **💡 Tip**: You can change devices during audio transfer
7. Press "Stop" to end the transfer

## 🏷️ Yeni Release Oluşturma / Creating New Release

**Türkçe:**
```bash
# Version tag'i oluşturun
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions otomatik olarak:
- Debug ve Release APK'larını oluşturur
- GitHub Release sayfası oluşturur
- APK'ları release'e ekler

**English:**
```bash
# Create version tag
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions automatically:
- Creates Debug and Release APKs
- Creates GitHub Release page
- Adds APKs to the release

Detaylar için / For details: [RELEASE.md](RELEASE.md)

## 🔄 GitHub Actions

Proje otomatik build ve release süreçleri içerir / Project includes automatic build and release processes:

- **Build APK**: Her push'ta otomatik APK oluşturur / Creates APK automatically on every push
- **Release**: Tag oluşturulduğunda otomatik release yapar / Creates automatic release when tag is created

## 🤝 Katkıda Bulunma / Contributing

**Türkçe:**
1. Projeyi fork edin
2. Feature branch oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi commit edin (`git commit -am 'Yeni özellik eklendi'`)
4. Branch'inizi push edin (`git push origin feature/yeni-ozellik`)
5. Pull Request oluşturun

**English:**
1. Fork the project
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## 📄 Lisans / License

Bu proje Apache 2.0 lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.

## ⚠️ Önemli Notlar / Important Notes

**Türkçe:**
- **Debug APK Kullanın**: Release APK imzasız olduğu için kurulum sorunları yaşayabilirsiniz
- Yüksek ses seviyelerinde geri besleme (feedback) oluşabilir
- Kulaklık kullanımı önerilir
- Bluetooth cihazlarda gecikme yaşanabilir

**English:**
- **Use Debug APK**: Release APK may have installation issues due to lack of signature
- Feedback may occur at high volume levels
- Headphone usage is recommended
- Bluetooth devices may experience latency

## 🐛 Sorun Bildirimi / Issue Reporting

**Türkçe:**
Sorun yaşıyorsanız [Issues](../../issues) sayfasından bildirebilirsiniz.

**English:**
If you experience any issues, you can report them on the [Issues](../../issues) page.