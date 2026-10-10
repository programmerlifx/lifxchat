# LIFX Chat — WhatsApp-style Android starter

Project Android Kotlin + Jetpack Compose dengan menu Chat, Status, Panggilan, Kontak, Profil, Pengaturan dan shortcut **Pesan ke diri sendiri**.

## Build APK via GitHub Actions
1. Upload seluruh isi ZIP ini ke repository GitHub (replace project lama bila memang ingin memakai starter ini).
2. Letakkan `google-services.json` dari Firebase Console di `app/google-services.json`.
3. Pastikan package aplikasi di Firebase adalah `com.lifx.chat`.
4. Buka tab **Actions** → **Build LIFX Chat APK** → **Run workflow**.
5. Setelah selesai, buka hasil run → **Artifacts** → unduh `LIFX-Chat-APK`.

## Catatan fitur
- Chat lokal, navigasi, kontak lokal, profil yang bisa diedit, status/panggilan sebagai halaman dasar, dan shortcut pesan diri sendiri sudah tersedia.
- Firebase SDK disiapkan. Penyimpanan pesan diri sendiri mencoba menyimpan ke Firestore jika Firebase terkonfigurasi.
- Chat antar-akun real-time, autentikasi lengkap, sinkronisasi kontak, media, push notification, enkripsi end-to-end, serta panggilan suara/video belum diimplementasikan. Jangan menganggapnya setara penuh dengan WhatsApp sampai backend dan fitur tersebut selesai.
