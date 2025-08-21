# KadimOyuncu-Plus

Kapsamlı oyuncu istatistikleri ve yönetim eklentisi.

---

## 📦 Genel Bilgiler

- **Eklenti Adı:** KadimOyuncu-Plus
- **Yazar:** ByNeels - Burak KAYA
- **Versiyon:** 1.2.0-SNAPSHOT
- **Ana Sınıf:** `me.kadim.KadimPlugin`

---

## 🔌 Yer Tutucular (Placeholders)

| Yer Tutucu | Açıklama |
|---|---|
| `%kplus_kd%` | Oyuncunun KD oranı |
| `%kplus_kills%` | Oyuncunun toplam öldürme sayısı |
| `%kplus_deaths%` | Oyuncunun toplam ölüm sayısı |
| `%kplus_kd_level_bar%` | KD oranına göre ilerleme çubuğu |
| `%kplus_kd_stars%` | KD oranına göre yıldızlı değerlendirme |
| `%kplus_kd_color%` | KD oranına göre renk kodu |
| `%kplus_playtime_total%` | Toplam aktiflik süresi |
| `%kplus_playtime_daily%` | Günlük aktiflik süresi |
| `%kplus_playtime_weekly%` | Haftalık aktiflik süresi |
| `%kplus_playtime_monthly%` | Aylık aktiflik süresi |
| `%kplus_top_overall_kd_X%` | Genel KD sıralamasında X. oyuncunun adı |
| `%kplus_top_overall_kdscore_X%` | Genel KD sıralamasında X. oyuncunun KD oranı |
| `%kplus_top_playtime_total_X%` | Genel aktiflik süresi sıralamasında X. sıradaki oyuncunun adı |
| `%kplus_top_playtime_total_score_X%` | Genel aktiflik süresi sıralamasında X. sıradaki oyuncunun süresi |
| ... (diğer tüm top yer tutucuları da 'kplus' ön ekini kullanır) |

---

## ⚙️ Komutlar

| Komut | Açıklama | Yetki |
|---|---|---|
| `/kd` | Kendi KD oranını gösterir | `kadimoyuncuplus.kd.use` |
| `/kd <oyuncuAdı>` | Belirtilen oyuncunun KD'sini gösterir | `kadimoyuncuplus.kd.use` |
| `/kdo` | KD ile ilgili seçenekleri gösterir | `kadimoyuncuplus.kdo.use` |
| `/kdo top` | En iyi KD oyuncularını sıralar | `kadimoyuncuplus.kdo.top` |
| `/kdo reset <oyuncuAdı>` | Oyuncunun KD verilerini sıfırlar | `kadimoyuncuplus.kdo.reset` |
| `/kplus reload` | Eklentiyi yeniden yükler | `kadimoyuncuplus.admin` |
| `/netherspawnayarla` | Nether için portal çıkış noktası belirler | `kadimoyuncuplus.nether.spawn.set` |

---

## 🔑 Yetkilendirmeler (Permissions)

Tüm yetkiler `kadimoyuncuplus.` ön eki ile başlar.

| Yetki | Açıklama | Varsayılan |
|---|---|---|
| `kadimoyuncuplus.admin` | /kplus ve reload işlemleri | `op` |
| `kadimoyuncuplus.kd.use` | /kd komutunu kullanma izni | `true` |
| `kadimoyuncuplus.kdo.top`| /kdo top komutunu kullanma izni | `true` |
| ... (diğer tüm yetkiler de `kadimoyuncuplus` ön eki ile devam eder) |

---

## 💾 Kalıcı Veri (data.yml)

Veriler şurada saklanır: `plugins/KadimOyuncu-Plus/data.yml`

```yaml
players:
  uuid:
    name: OyuncuAdı
    kills: 10
    deaths: 3
    daily:
      YYYY-MM-DD:
        kills: 5
        deaths: 2
    weekly:
      YYYY-ww:
        kills: 12
        deaths: 5
    monthly:
      YYYY-MM:
        kills: 25
        deaths: 10
    playtime:
      total: 36000
      daily:
        YYYY-MM-DD: 7200
      weekly:
        YYYY-ww: 25200
      monthly:
        YYYY-MM: 108000
    nether_data:
      last_far_location: