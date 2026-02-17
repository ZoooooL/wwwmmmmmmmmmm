## Odoo + OpenAI Connection Setup

المستودع يحتوي:
1) سكربت Python لفحص الاتصال.
2) تطبيق Android داخل `android-app/` قابل لإخراج APK (Debug/Release Signed).

---

## Python verifier

### إعداد وتشغيل
```bash
cp .env.example .env
python3 scripts/verify_connections.py
```

### اختبارات السكربت
```bash
python3 -m py_compile scripts/verify_connections.py
python3 -m unittest discover -s tests -v
```

---

## Android APK

### مكان التطبيق
- `android-app/`
- package: `com.example.odooopenaichecker`

### Debug APK
- محليًا (Android Studio): افتح `android-app` ثم Build APK(s).
- عبر GitHub Actions: workflow `Build Android APK` يرفع `app-debug.apk`.

---

## Release Signed APK (جاهز للتثبيت)

### 1) إنشاء keystore محليًا
```bash
keytool -genkeypair \
  -v \
  -keystore android-app/keystore/release-key.jks \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2) إعداد توقيع محلي
```bash
cp android-app/keystore.properties.example android-app/keystore.properties
```

ثم عدّل القيم:
- `KEYSTORE_FILE=keystore/release-key.jks`
- `KEYSTORE_PASSWORD=...`
- `KEY_ALIAS=release`
- `KEY_PASSWORD=...`

### 3) بناء Release APK محليًا
```bash
gradle -p android-app :app:assembleRelease
```

الملف الناتج:
- `android-app/app/build/outputs/apk/release/app-release.apk`

### 4) بناء Release Signed APK عبر GitHub Actions
أضف Secrets في GitHub Repository:
- `KEYSTORE_BASE64` (محتوى ملف `.jks` بعد تحويله base64)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

ثم شغّل workflow:
- `.github/workflows/android-release.yml`

وسيرفع artifact باسم `app-release-apk`.

---

## تنبيه أمني مهم
أي مفاتيح/API Keys تمت مشاركتها علنًا يجب تدويرها فورًا.
وكذلك يجب حفظ `keystore` وبيانات التوقيع في مكان آمن جدًا.
