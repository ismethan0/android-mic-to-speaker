# 🚀 Release Oluşturma Rehberi / Release Creation Guide

Bu rehber, Mikrofon Hoparlör uygulaması için yeni bir release oluşturma sürecini açıklar.

This guide explains the process of creating a new release for the Microphone Speaker app.

## 📋 Release Oluşturma Adımları / Release Creation Steps

### 1. Kod Hazırlığı / Code Preparation

**Türkçe:**
```bash
# Son değişiklikleri commit edin
git add .
git commit -m "Release hazırlığı: v1.0.0"
git push origin main
```

**English:**
```bash
# Commit the latest changes
git add .
git commit -m "Release preparation: v1.0.0"
git push origin main
```

### 2. Tag Oluşturma / Tag Creation

**Türkçe:**
```bash
# Yeni version tag'i oluşturun
git tag v1.0.0
git push origin v1.0.0
```

**English:**
```bash
# Create new version tag
git tag v1.0.0
git push origin v1.0.0
```

### 3. Otomatik Release / Automatic Release

**Türkçe:**
Tag push edildiğinde GitHub Actions otomatik olarak:
- ✅ Debug APK oluşturur (kolay kurulum)
- ✅ Release APK oluşturur (optimize edilmiş)
- ✅ GitHub Release sayfası oluşturur
- ✅ Her iki APK'yı release'e ekler

**English:**
When the tag is pushed, GitHub Actions automatically:
- ✅ Creates Debug APK (easy installation)
- ✅ Creates Release APK (optimized)
- ✅ Creates GitHub Release page
- ✅ Adds both APKs to the release

## 📱 APK Türleri / APK Types

### Debug APK (Önerilen / Recommended) ✅

**Türkçe:**
- **Dosya**: `microphone-speaker-v1.0.0-debug.apk`
- **Avantaj**: Debug key ile imzalı, kolay kurulum
- **Kullanım**: Test ve normal kullanım için ideal
- **⚠️ ÖNEMLİ**: Bu APK'yı kullanın, kurulum sorunu yaşamazsınız

**English:**
- **File**: `microphone-speaker-v1.0.0-debug.apk`
- **Advantage**: Signed with debug key, easy installation
- **Usage**: Ideal for testing and normal use
- **⚠️ IMPORTANT**: Use this APK, you won't experience installation issues

### Release APK (Önerilmez / Not Recommended) ⚠️

**Türkçe:**
- **Dosya**: `microphone-speaker-v1.0.0-release-unsigned.apk`
- **Avantaj**: Optimize edilmiş, daha küçük boyut
- **Problem**: İmzasız olduğu için kurulum sorunları yaşayabilirsiniz
- **Not**: "Bilinmeyen kaynaklardan yükleme" bile yeterli olmayabilir

**English:**
- **File**: `microphone-speaker-v1.0.0-release-unsigned.apk`
- **Advantage**: Optimized, smaller size
- **Problem**: May have installation issues due to lack of signature
- **Note**: Even "Install from unknown sources" may not be sufficient

## 🔐 APK İmzalama (Opsiyonel) / APK Signing (Optional)

**Türkçe:**
Release APK'sını imzalamak için repository secrets'a ekleyin:

**English:**
To sign the Release APK, add to repository secrets:

```
KEYSTORE_BASE64=<keystore dosyasının base64 hali / base64 of keystore file>
KEYSTORE_PASSWORD=<keystore şifresi / keystore password>
KEY_ALIAS=<key alias>
KEY_PASSWORD=<key şifresi / key password>
```

**Türkçe:**
İmzalı APK: `microphone-speaker-v1.0.0-release-signed.apk`

**English:**
Signed APK: `microphone-speaker-v1.0.0-release-signed.apk`

## 📝 Version Numaralandırma / Version Numbering

**Türkçe:**
Semantic Versioning kullanın:
- `v1.0.0` - İlk stabil release
- `v1.0.1` - Bug fix
- `v1.1.0` - Yeni özellik
- `v2.0.0` - Breaking change

**English:**
Use Semantic Versioning:
- `v1.0.0` - First stable release
- `v1.0.1` - Bug fix
- `v1.1.0` - New feature
- `v2.0.0` - Breaking change

## 🎯 Release Checklist

**Türkçe:**
- [ ] Kod testleri geçiyor
- [ ] Uygulama gerçek cihazda test edildi
- [ ] Version numarası güncellendi
- [ ] CHANGELOG.md güncellendi
- [ ] Tag oluşturuldu ve push edildi
- [ ] GitHub Actions başarıyla tamamlandı
- [ ] APK'lar release sayfasında mevcut
- [ ] Debug APK test edildi ve çalışıyor

**English:**
- [ ] Code tests are passing
- [ ] App tested on real device
- [ ] Version number updated
- [ ] CHANGELOG.md updated
- [ ] Tag created and pushed
- [ ] GitHub Actions completed successfully
- [ ] APKs available on release page
- [ ] Debug APK tested and working

## 🔄 Manuel Release (Alternatif) / Manual Release (Alternative)

**Türkçe:**
GitHub Actions'ı manuel olarak çalıştırmak için:

1. GitHub repo → Actions sekmesi
2. "Release APK" workflow'unu seçin
3. "Run workflow" butonuna tıklayın
4. Version tag'ini girin (örn: v1.0.0)
5. "Run workflow" ile başlatın

**English:**
To run GitHub Actions manually:

1. GitHub repo → Actions tab
2. Select "Release APK" workflow
3. Click "Run workflow" button
4. Enter version tag (e.g., v1.0.0)
5. Start with "Run workflow"

## 📞 Sorun Giderme / Troubleshooting

### APK Kurulmuyor / APK Won't Install

**Türkçe:**
- **Çözüm**: Debug APK'yı kullanın
- **Alternatif**: "Bilinmeyen kaynaklardan yükleme" iznini verin
- **Son çare**: APK'yı imzalayın

**English:**
- **Solution**: Use Debug APK
- **Alternative**: Enable "Install from unknown sources"
- **Last resort**: Sign the APK

### GitHub Actions Başarısız / GitHub Actions Failed

**Türkçe:**
- **Çözüm**: Actions sekmesinden log'ları kontrol edin
- **Kontrol**: Gradle build hatalarını inceleyin
- **Test**: Yerel olarak build alın

**English:**
- **Solution**: Check logs from Actions tab
- **Check**: Review Gradle build errors
- **Test**: Build locally

### Tag Push Edilmiyor / Tag Not Pushing

**Türkçe:**
- **Çözüm**: `git push origin --tags` komutunu kullanın
- **Kontrol**: Tag'in doğru oluşturulduğunu kontrol edin
- **Test**: `git tag -l` ile tag'leri listeleyin

**English:**
- **Solution**: Use `git push origin --tags` command
- **Check**: Verify the tag was created correctly
- **Test**: List tags with `git tag -l`

## 💡 Öneriler / Recommendations

**Türkçe:**
1. **Her zaman Debug APK kullanın** - Kurulum sorunu yaşamazsınız
2. Release APK sadece imzalı versiyonunu kullanın
3. Test cihazında önce deneyin
4. Version notlarını detaylı yazın

**English:**
1. **Always use Debug APK** - You won't experience installation issues
2. Use Release APK only if it's signed
3. Test on a test device first
4. Write detailed version notes

## 🔗 Faydalı Linkler / Useful Links

- [Android APK Signing](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Semantic Versioning](https://semver.org/)