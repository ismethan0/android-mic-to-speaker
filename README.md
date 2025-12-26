# Mikrofon Hoparlör Uygulaması

![Build Status](https://github.com/[KULLANICI_ADI]/[REPO_ADI]/workflows/Build%20APK/badge.svg)

Mikrofondan gelen sesi gerçek zamanlı olarak hoparlöre aktaran basit Android uygulaması.

## 🎤 Özellikler

- **Gerçek Zamanlı Ses Aktarımı**: Mikrofondan gelen ses anında hoparlöre aktarılır
- **Çoklu Cihaz Desteği**: 
  - Dahili mikrofon
  - Bluetooth mikrofon
  - Dahili hoparlör  
  - Bluetooth hoparlör
- **Kolay Kullanım**: Basit ve sezgisel arayüz
- **Modern Tasarım**: Jetpack Compose ile oluşturulmuş modern UI

## 📱 Ekran Görüntüleri

<img src="screenshots/phone_my_garden.png" width="300"/>

## 🚀 Kurulum

### APK İndirme
1. [Releases](../../releases) sayfasından en son APK'yı indirin
2. Android cihazınızda "Bilinmeyen kaynaklardan yükleme" iznini etkinleştirin
3. APK dosyasını yükleyin

### Kaynak Koddan Derleme
```bash
git clone https://github.com/[KULLANICI_ADI]/[REPO_ADI].git
cd [REPO_ADI]
./gradlew assembleDebug
```

## 🔧 Gereksinimler

- **Android 7.0** (API 24) ve üzeri
- **Mikrofon erişim izni**
- **Ses ayarları değiştirme izni**
- **Bluetooth erişim izni** (Bluetooth cihazlar için)

## 📋 İzinler

Uygulama aşağıdaki izinleri gerektirir:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## 🛠️ Teknolojiler

- **Kotlin** - Ana programlama dili
- **Jetpack Compose** - Modern UI toolkit
- **Hilt** - Dependency injection
- **AudioRecord/AudioTrack** - Ses kayıt ve çalma
- **MVVM Architecture** - Mimari deseni

## 📖 Kullanım

1. Uygulamayı açın
2. Kullanmak istediğiniz mikrofonu seçin
3. Ses çıkışı için hoparlörü seçin
4. "Başlat" butonuna basın
5. Mikrofondan gelen ses hoparlöre aktarılmaya başlar
6. "Durdur" butonuna basarak aktarımı sonlandırın

## 🔄 GitHub Actions

Proje otomatik build ve release süreçleri içerir:

- **Build APK**: Her push'ta otomatik APK oluşturur
- **Release**: Tag oluşturulduğunda otomatik release yapar

### Release Oluşturma
```bash
git tag v1.0.0
git push origin v1.0.0
```

## 🤝 Katkıda Bulunma

1. Projeyi fork edin
2. Feature branch oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi commit edin (`git commit -am 'Yeni özellik eklendi'`)
4. Branch'inizi push edin (`git push origin feature/yeni-ozellik`)
5. Pull Request oluşturun

## 📄 Lisans

Bu proje Apache 2.0 lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## ⚠️ Uyarılar

- Yüksek ses seviyelerinde geri besleme (feedback) oluşabilir
- Kulaklık kullanımı önerilir
- Bluetooth cihazlarda gecikme yaşanabilir

## 🐛 Sorun Bildirimi

Sorun yaşıyorsanız [Issues](../../issues) sayfasından bildirebilirsiniz.
