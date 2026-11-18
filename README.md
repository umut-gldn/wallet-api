# wallet-api

[![CI](https://github.com/umut-gldn/wallet-api/actions/workflows/ci.yml/badge.svg)](https://github.com/umut-gldn/wallet-api/actions/workflows/ci.yml)

Dijital cüzdan / para transferi backend'i. Kullanıcılar kayıt olur, hesap açar, para yatırır-çeker ve hesaplar arası transfer yapar.

Amaç bir CRUD örneği değil: **para taşıyan bir sistemde ortaya çıkan gerçek sorunları (yarış koşulları, çift işlem, yetkisiz erişim, şema evrimi) kendim yaşayıp testle kanıtlayarak çözmek.**

## Teknoloji

Java 21 · Spring Boot 4.1.1 · Spring Security (OAuth2 Resource Server, JWT) · Spring Data JPA · PostgreSQL 17 · Flyway · Testcontainers · springdoc-openapi (Swagger UI) · Docker · GitHub Actions · Maven · Lombok

## Çalıştırma

**Tek komutla (sadece Docker gerekir):**

```bash
docker compose up --build   # PostgreSQL + uygulamayı derleyip başlatır → http://localhost:8080
```

**Geliştirme modu (uygulamayı IDE'den çalıştırmak için):**

```bash
docker compose up -d postgres   # Sadece PostgreSQL'i başlatır (localhost:5432, db/kullanıcı/şifre: wallet)
./mvnw spring-boot:run          # Uygulamayı başlatır; Flyway şemayı otomatik kurar
./mvnw verify                   # Tüm testleri çalıştırır (Docker gerekir, Testcontainers kullanır)
```

Windows PowerShell'de `./mvnw` yerine `.\mvnw.cmd` kullan.

Tüm uçları tarayıcıdan denemek için Swagger arayüzü: http://localhost:8080/swagger-ui.html (önce `login` ile token al, sonra **Authorize**'a yapıştır).

JWT imza anahtarı `JWT_SECRET` ortam değişkeniyle verilir (en az 32 byte). Tanımlı değilse yalnızca geliştirme için bir varsayılan kullanılır.

CI (GitHub Actions) her push'ta tüm testleri çalıştırır.

## API

| Method | Yol | Açıklama |
|---|---|---|
| POST | `/api/v1/auth/register` | Kayıt (e-posta + şifre) |
| POST | `/api/v1/auth/login` | Giriş, JWT döner |
| POST | `/api/v1/accounts` | Hesap aç (sahip token'dan gelir) |
| GET | `/api/v1/accounts/{id}` | Hesap bilgisi |
| POST | `/api/v1/accounts/{id}/deposits` | Para yatır |
| POST | `/api/v1/accounts/{id}/withdrawals` | Para çek |
| GET | `/api/v1/accounts/{id}/entries` | Hesap hareketleri (offset sayfalama) |
| GET | `/api/v1/accounts/{id}/entries/cursor` | Hesap hareketleri (cursor sayfalama) |
| POST | `/api/v1/transfers` | Transfer (`Idempotency-Key` header'ı zorunlu) |

`/auth/**` dışındaki tüm uçlar `Authorization: Bearer <token>` ister. Hatalar RFC 9457 `ProblemDetail` biçiminde, makine tarafından okunabilir bir `code` alanıyla döner.

## Tasarım kararları ve kanıtları

Her karar bir sorundan doğdu; sorun önce testle yeniden üretildi, sonra çözüldü.

### 0. Defter (ledger) ve bakiye tutarlılığı
- **Sorun:** Sadece bakiye tutan bir tabloda bir şey ters giderse paranın nereye gittiği bilinemez.
- **Çözüm:** Her bakiye değişikliği, silinmeyen (yalnızca eklenen) bir hareket kaydı (`account_entry`) üretir. Bakiye ve kayıt **aynı transaction'da** yazılır: ya ikisi birlikte kaydedilir ya da ikisi birlikte geri alınır. Her kayıt işlem sonrası bakiyeyi (`balanceAfter`) de taşır. Hareket yönü `EntryType` enum'unun içindeki `increasesBalance()` kuralıyla belirlenir; yeni bir tür eklendiğinde yönü açıkça belirtmek zorunlu olur, yanlışlıkla "azaltan" tarafa düşemez.
- **Para tipi:** `BigDecimal` (ondalık hata yok), karşılaştırmada `compareTo`.
- **Kanıt:** `ConcurrentTransferIT` (100 paralel transferden sonra toplam para korunur)

### 1. Eşzamanlı işlemler: kayıp güncelleme
- **Sorun:** İki işlem aynı bakiyeyi aynı anda okuyup birbirinin üstüne yazınca para kayboluyor. Testte bakiye 1000 olması gerekirken **1030** çıktı. Kilitsiz paralel çekimlerde 8 işlem başarıyla geçti ve bakiye kuralını ihlal etti.
- **Çözüm:** `SELECT ... FOR UPDATE` (pessimistic write lock) ile satır kilidi. Kilit alındıktan sonra bakiye yeniden okunur; kilitten önce yüklenmiş eski entity kopyasına güvenilmez.
- **Kanıt:** `ConcurrentWithdrawalIT`

### 2. Karşılıklı transferlerde deadlock
- **Sorun:** A→B ve B→A transferleri paralel çalışınca iki işlem birbirinin kilidini bekleyip `deadlock detected` hatası verdi.
- **Çözüm:** Hesaplar her zaman **id sırasına göre** kilitlenir. Herkes aynı sırada kilitlediği için döngü oluşmaz.
- **Kanıt:** `ConcurrentTransferIT` (100 paralel, karşılıklı transfer; toplam para korunur)

### 3. Idempotency (çift işlem koruması)
- **Sorun:** Ağ hatasında istemci isteği tekrar gönderirse para iki kez taşınabilir.
- **Çözüm:** `Idempotency-Key` header'ı. Aynı anahtar + aynı istek aynı sonucu döner (replay), aynı anahtar + farklı istek reddedilir. Yarışı uygulama değil veritabanı çözer: `(owner_id, idempotency_key)` unique constraint'i. Anahtarlar kullanıcıya özeldir; iki kullanıcı aynı anahtarı birbirini etkilemeden kullanabilir.
- **Kanıt:** `ConcurrentTransferIT` (aynı anahtarla 10 paralel istek → para yalnızca bir kez taşınır), `TransferOwnershipIT`

### 4. Sayfalama: offset yerine cursor (keyset)
- **Sorun:** Offset sayfalamada iki sayfa arasında yeni kayıt gelirse liste kayar ve aynı kayıt iki kez görünür. Ayrıca büyük offset'lerde veritabanı atlanan satırları da okumak zorunda kalır.
- **Çözüm:** `(createdAt, id)` ikilisine dayalı cursor. Cursor opak (Base64) bir değerdir; istemci içeriğini bilmez. `size + 1` kayıt çekilerek `count(*)` olmadan sonraki sayfanın varlığı anlaşılır. `id`, aynı zaman damgalı kayıtlarda sıralamayı belirsizlikten kurtarır. Bozuk cursor `400 INVALID_CURSOR` döner. Sayfa boyutu üst sınırı 100'dür.
- **Kanıt:** `AccountEntryPaginationIT` (offset'te tekrar eden kayıt görünür, cursor'da görünmez)

### 5. Kimlik doğrulama ve yetkilendirme (IDOR koruması)
- **Kimlik:** BCrypt ile şifre hash'i; HS256 imzalı JWT. Kullanıcı kimliği `sub` alanından gelir, istek gövdesinden asla alınmaz. Sahip bilgisi gövdede gönderilse bile yok sayılır.
- **Yetki:** Başkasının hesabına erişim `403` değil **`404`** döner; böylece bir hesabın var olup olmadığı sızmaz. Kontrol servis katmanında sahip bilgisiyle yapılır (`findByIdAndOwnerId`). Yetki kontrolü kilitten önce `exists` sorgusuyla yapılır; bu sayede eski entity kopyası sorunu tekrar etmez.
- **Hata biçimi:** Güvenlik filtresinde oluşan 401 hataları da `GlobalExceptionHandler`'a yönlendirilir; tüm hatalar aynı `ProblemDetail` biçimindedir.
- **E-posta normalizasyonu:** İstek sınırında `trim + toLowerCase(Locale.ROOT)`. `Locale.ROOT` bilinçli seçim: Türkçe locale'de `I` harfi `ı` olur ve aynı e-posta farklı kayıt sayılabilir.
- **Kanıt:** `AuthSecurityIT` (token yok, sahte token, sahte `sub`, sahip token'dan gelir, başkasının hesabı 404, e-posta büyük/küçük harf), `AccountOwnershipIT`, `TransferOwnershipIT`

### 6. Şema yönetimi: `ddl-auto` yerine Flyway
- **Sorun:** `ddl-auto=update` mevcut enum CHECK constraint'lerini güncellemez. Yeni enum değeri eklenince eski veritabanında `account_entry_type_check` ihlali alındı. Dolu tabloya NOT NULL kolon da ekleyemez, eski unique constraint'leri de silmez.
- **Çözüm:** Flyway versiyonlu migration'lar (`src/main/resources/db/migration`), Hibernate `ddl-auto: validate` ile yalnızca doğrulama yapar. V2 ile veritabanı seviyesinde bütünlük kuralları eklendi: yabancı anahtarlar, `amount > 0`, `from <> to`, `balance >= 0`. V3 ile `lower(email)` üzerinde unique index eklendi; uygulama e-postayı normalize etse bile veritabanı büyük/küçük harf farkıyla ikinci kayda izin vermez.
- **Kural:** Uygulanmış bir migration asla değiştirilmez; Flyway checksum uyuşmazlığıyla bunu deneyerek doğruladım. Değişiklik yeni bir `V<n>` dosyasıyla yapılır.
- **Kanıt:** Testlerin hepsi Testcontainers ile boş bir PostgreSQL üzerinde Flyway'i çalıştırır ve `validate` ile şemayı doğrular.

## Postman koleksiyonu

[`postman/wallet-api.postman_collection.json`](postman/wallet-api.postman_collection.json) dosyasını Postman'e aktar (Import), uygulama çalışırken **Run** ile sırayla çalıştır. 40 isteğin her birinde otomatik test var: kayıt/giriş, hesap işlemleri, idempotency, sayfalama, başkasının hesabına erişim denemeleri. Her çalıştırmada e-postalar yeniden üretilir, istediğin kadar tekrar çalıştırabilirsin.

## Testler

Testler gerçek PostgreSQL üzerinde (Testcontainers) çalışır; H2 gibi bir taklit kullanılmaz, çünkü kilitler, constraint'ler ve sıralama davranışı veritabanına özgüdür.

| Sınıf | Neyi kanıtlar |
|---|---|
| `ConcurrentWithdrawalIT` | Paralel çekimde bakiye tutarlılığı |
| `ConcurrentTransferIT` | Deadlock yok, toplam para sabit, idempotency yarışı |
| `AccountEntryPaginationIT` | Offset kayması, cursor kararlılığı, bozuk cursor |
| `AccountOwnershipIT` / `TransferOwnershipIT` | Başkasının hesabına erişilemez |
| `AuthSecurityIT` | 401 biçimi, sahte token, sahip token'dan gelir |
| `EmailNormalizationTest` | E-posta normalizasyonu (Türkçe locale dahil) |
| `LuhnTest` | Hesap numarası kontrol hanesi (Luhn) |
| `WalletApiApplicationTests` | Uygulama, Flyway + `validate` ile boş bir veritabanında ayağa kalkar |

## Bilinen sınırlar

Bilinçli olarak kapsam dışı bırakılanlar ve gerçek bir ortamda yapılacaklar:

- **Token iptal edilemez.** JWT stateless olduğu için çıkış yapılsa veya kullanıcı kapatılsa bile süresi bitene kadar (1 saat) geçerlidir. Çözüm: ömrü ~15 dakikaya indirip refresh token eklemek ve/veya kritik uçlarda kullanıcının aktifliğini veritabanından doğrulamak.
- **Login için rate limit yok.** Kaba kuvvet denemelerine açık.
- **Kayıt uç noktası e-postanın kayıtlı olduğunu ele verir** (409 yanıtı ve zamanlama farkı).
- **Docker compose'daki `JWT_SECRET` ve veritabanı şifresi örnek değerlerdir;** gerçek ortamda secret yönetimi kullanılmalı.
- **`JWT_SECRET` gerçek ortamda ortam değişkeni / secret manager ile verilmeli;** koddaki varsayılan yalnızca geliştirme içindir. Geliştirme veritabanı kimlik bilgileri (`wallet/wallet`) de öyle.
- **Rol yok.** Tüm kullanıcılar eşit; admin/yetki katmanı yok.
- **Para birimi dönüşümü yok.** Transfer yalnızca aynı para birimindeki hesaplar arasında yapılır.
