## Odoo + OpenAI Connection Setup

تمت مراجعة السكربت وتحسينه ليكون جاهزًا للرفع على GitHub بشكل أفضل:
- تحميل تلقائي من `.env` بدون مكتبات خارجية.
- Timeout للشبكة حتى لا يعلق السكربت.
- إظهار مفاتيح بشكل مخفي (Masked) في الرسائل.
- اختبارات وحدة + CI على GitHub Actions.

---

## 1) إعداد المتغيرات

انسخ ملف القالب ثم أضف بياناتك:

```bash
cp .env.example .env
```

املأ القيم التالية:
- `ODOO_URL`
- `ODOO_DB`
- `ODOO_USERNAME`
- `ODOO_API_KEY`
- `OPENAI_API_KEY`

> ملاحظة: لا ترفع `.env` على GitHub.

---

## 2) تشغيل فحص الاتصال

```bash
python3 scripts/verify_connections.py
```

خيارات إضافية:

```bash
python3 scripts/verify_connections.py --timeout 30
python3 scripts/verify_connections.py --env-file .env.production
python3 scripts/verify_connections.py --skip-env-file
```

> ملاحظة: قيمة `--timeout` يجب أن تكون أكبر من 0.

سيعرض السكربت JSON يحتوي نتيجة كل خدمة.

---

## 3) الاختبارات المحلية

```bash
python3 -m py_compile scripts/verify_connections.py
python3 -m unittest discover -s tests -v
```

---

## 4) الرفع على GitHub

بعد كل Push/PR سيتم تشغيل CI تلقائيًا من الملف:
- `.github/workflows/ci.yml`

---

## 5) بخصوص APK

المستودع الحالي لا يحتوي مشروع Android/Flutter كامل لتوليد APK مباشرة.

لإنشاء APK تحتاج واحدًا من المسارين:
1. إنشاء تطبيق Android (Kotlin/Java) أو Flutter.
2. دمج منطق الربط داخل التطبيق ثم بناء APK عبر Android Studio أو CI.

إذا أردت، أستطيع في الخطوة التالية تجهيز لك هيكل مشروع Flutter/Android جاهز للبناء إلى APK.

---

## تنبيه أمني

المفاتيح التي تمت مشاركتها علنًا يجب اعتبارها مكشوفة، ويجب تدويرها (Rotate) فورًا:
- Odoo API Key
- OpenAI API Key
