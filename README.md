# R7XTiertagger

R7XTiertagger, Minecraft'ta oyuncu adlarının yanında R7XTiers PvP sıralamalarını gösteren istemci taraflı bir Fabric modudur.

Oyuncu sıralamalarını [`r7xtierlist.tr` API'sinden](https://r7xtierlist.tr/api/players) alır ve Minecraft oyuncu adlarıyla eşleştirir. Oyun modlarının tierlerini oyun içi ikonlarla gösterir; oyuncu arama ve profil ekranı ile `/tiertagger <player>` komutunu içerir.

## Minecraft sürümleri

- Minecraft 1.21–1.21.4: sürüme özel JAR gerekir.
- Minecraft 1.21.5–1.21.11: tek evrensel JAR desteklenir.

## Derleme

Windows'ta PowerShell'i depo klasöründe açıp uygun komutu çalıştır:

```powershell
# 1.21, 1.21.1, 1.21.2, 1.21.3 ve 1.21.4 için ayrı JAR'lar
.\build-1.21-1.21.4.ps1

# 1.21.5–1.21.11 için sürüme özel JAR'lar
.\build-1.21.5-1.21.11.ps1

# 1.21.5–1.21.11 için tek evrensel JAR
.\build-universal-1.21.5-1.21.11.ps1
```

JAR dosyaları `build/libs` klasörüne yazılır. Evrensel JAR için `+universal.jar` dosyasını paylaş. Aynı modun birden fazla JAR'ını aynı `mods` klasörüne koyma.

## Lisans ve kaynak

Bu proje MPL-2.0 lisanslıdır; ayrıntılar için [`LICENSE`](LICENSE) dosyasına bak. Kaynak kodu [GitHub deposunda](https://github.com/r7xtierlist-eng/R7XTierTaagger) yayımlanır. Proje, orijinal TierTagger çalışmasının devamıdır; orijinal proje ve yazar bilgileri mod metadata'sında korunmuştur.

---

# R7XTiertagger (English)

A client-side Fabric mod that displays R7XTiers PvP rankings beside Minecraft player names. It fetches rankings from the [`r7xtierlist.tr` API](https://r7xtierlist.tr/api/players), displays game mode tier icons, and includes player search, a profile screen, and the `/tiertagger <player>` command.

See the Minecraft version and build instructions above. The project is licensed under MPL-2.0; see [`LICENSE`](LICENSE). Source code is published in the [GitHub repository](https://github.com/r7xtierlist-eng/R7XTierTaagger). This is a continuation of the original TierTagger project, whose attribution is preserved in the mod metadata.
