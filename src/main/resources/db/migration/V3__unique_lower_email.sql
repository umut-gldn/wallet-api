-- E-posta büyük/küçük harf farkıyla ikinci kez kaydedilemesin (uygulama normalizasyonuna ek, veritabanı güvencesi)
CREATE UNIQUE INDEX uk_app_user_email_lower ON app_user (lower(email));