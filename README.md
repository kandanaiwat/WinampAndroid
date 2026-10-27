# Winamp for Android (Kotlin + Jetpack Compose)

## วิธีรัน
1. เปิดโฟลเดอร์นี้ด้วย Android Studio (Koala หรือใหม่กว่า) แล้วรอ Gradle Sync
2. กด Run บนมือถือ/อีมูเลเตอร์ (minSdk 24)
3. กด ⏏ หรือ ADD เพื่อเลือกไฟล์เพลง, กด SKINS เพื่อเปลี่ยนสกิน

## เพิ่มสกินใหม่
แก้ไฟล์ Skin.kt เพิ่ม Skin(...) ในลิสต์ Skins อีกหนึ่งบรรทัด

## สร้างไฟล์ APK
**วิธี A (Android Studio):** เมนู Build > Build Bundle(s) / APK(s) > Build APK(s)
ไฟล์อยู่ที่ app/build/outputs/apk/debug/app-debug.apk

**วิธี B (ไม่ต้องลงโปรแกรม):** อัปโหลดโปรเจกต์ขึ้น GitHub repo ของคุณ
แล้วเข้าแท็บ Actions > Build APK > ดาวน์โหลด Winamp-debug-apk
