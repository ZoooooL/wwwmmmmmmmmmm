## Odoo + OpenAI Connection Setup

المستودع الآن يحتوي جزئين:
1) سكربت Python لفحص الاتصال.
2) تطبيق Android (Kotlin) داخل `android-app/` يمكن بناءه إلى APK.

---

## Python verifier

### 1) إعداد المتغيرات
```bash
cp .env.example .env
```

### 2) تشغيل الفحص
```bash
python3 scripts/verify_connections.py
```

### 3) اختبارات السكربت
```bash
python3 -m py_compile scripts/verify_connections.py
python3 -m unittest discover -s tests -v
```

---

## Android APK

### مكان التطبيق
- `android-app/`
- الحزمة: `com.example.odooopenaichecker`

### ماذا يفعل التطبيق؟
- شاشة إدخال بيانات Odoo + OpenAI.
- زر **Check Connections**.
- التحقق من:
  - Odoo عبر `xmlrpc/2/common` (authenticate)
  - OpenAI عبر `GET /v1/models`
- عرض النتيجة مباشرة داخل التطبيق.

### بناء APK محليًا (Android Studio)
1. افتح مجلد `android-app` في Android Studio.
2. انتظر مزامنة Gradle.
3. Build > Build APK(s).

### بناء APK عبر GitHub Actions
أضفنا Workflow جاهز:
- `.github/workflows/android-apk.yml`

يمكنك تشغيله يدويًا من تبويب Actions، وسيتم رفع ملف:
- `app-debug.apk` كـ artifact.

---

## تنبيه أمني مهم
أي مفاتيح تمت مشاركتها علنًا تعتبر مكشوفة ويجب تدويرها فورًا:
- Odoo API Key
- OpenAI API Key
