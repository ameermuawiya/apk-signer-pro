<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="140" alt="APK Signer Pro Icon"/>

# APK Signer Pro

### Sign Android APKs Easily & Securely

An open-source Android toolkit for signing, verifying, extracting, backing up, and managing APK packages directly on your device.

<img src="https://i.ibb.co/67chvKHX/upload-1791255917747.png" width="100%" alt="APK Signer Pro"/>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat)](LICENSE)
[![XERJ Corpus Hub](https://hub.xerj.org/contributors/ameermuawiya/badge.svg)](https://hub.xerj.org/contributors/ameermuawiya.html)

</div>

---

## ✨ Key Features

APK Signer Pro combines advanced APK signing capabilities with a modern interface built using Kotlin and Jetpack Compose.

### 🔐 APK Signing & Verification

- **v1 APK Signing:** Legacy JAR-based APK signing for older Android versions.
- **v2 APK Signing & Verification:** Full-file binary integrity protection for Android 7.0+.
- **v3 APK Signing & Verification:** Key rotation and cryptographic lineage support for Android 9.0+.
- **v4 APK Signing:** Streaming signature generation with `.apk.idsig` support for Android 11.0+.
- **16KB & 4KB Page Alignment:** Aligns native `.so` libraries and `resources.arsc` for modern Android compatibility.
- **Multi-Format Keystores:** Supports `JKS`, `PKCS12`, `BKS`, `BKS-V1`, `UBER`, and `BCFKS`.
- **Built-in Debug Key:** Quickly sign applications using standard Android Studio debug credentials.
- **Certificate Inspection:** View certificate algorithm, validity, subject, issuer, and SHA-256 fingerprint.
- **Persistent Credentials:** Save key associations for faster signing without repeated prompts.

### 📦 APK & Package Management

- **APK Backup & Extraction:** Back up and extract APKs from installed user and system applications.
- **Split APK Support:** Handle split APK packages and `.apks` files.
- **XAPK Support:** Work with XAPK packages alongside standard APK and APKS files.
- **APK Management:** Install, share, copy paths, and remove generated packages.
- **Signing History:** Keep track of previous signing operations.
- **Search & Filtering:** Quickly find records and installed applications.
- **Organized Storage:** Automatically organize signed packages and backups.

### 💾 Storage & File Handling

- **Reactive Local Storage:** SQLite persistence with Kotlin Flow.
- **Secure File Handling:** Uses Android's `FileProvider` and system sharing mechanisms.

---

## 🎨 Material 3 Expressive UI

APK Signer Pro features a modern interface designed around Material 3 Expressive principles.

- **Material 3 Expressive:** Modern interface built entirely with Jetpack Compose.
- **Dynamic Colors:** Adapts to the system wallpaper color palette.
- **Light & Dark Themes:** Supports system-based theme modes.
- **Expressive Loading Indicators:** Smooth loading indicators across different screens and actions.
- **Pull-to-Refresh:** Expressive swipe-to-refresh interactions.
- **Collapsing Top App Bars:** Smooth nested scrolling across primary screens.
- **Live Logs:** Colorized signing and execution logs with step indicators.
- **One-Tap Log Copying:** Quickly copy logs to the clipboard.
- **Consistent Empty States:** Clean and polished empty-state layouts.
- **Responsive Layouts:** Designed for a consistent experience across Android devices.
- **Multi-language Support:** Available in English, Chinese, Russian, Portuguese, French, Spanish, German, and Arabic.

---

## 📸 Screenshots

<div align="center">

<img src="screenshots/1.jpg" width="48%" alt="APK Signer Pro Home"/>
<img src="screenshots/2.jpg" width="48%" alt="APK Signer Pro History"/>

<img src="screenshots/3.jpg" width="48%" alt="APK Signer Pro Signing"/>
<img src="screenshots/4.jpg" width="48%" alt="APK Signer Pro Keystore"/>

<img src="screenshots/5.jpg" width="48%" alt="APK Signer Pro Settings"/>
<img src="screenshots/6.jpg" width="48%" alt="APK Signer Pro Logs"/>

<img src="screenshots/7.jpg" width="48%" alt="APK Signer Pro Interface"/>
<img src="screenshots/8.jpg" width="48%" alt="APK Signer Pro Interface"/>

</div>

---

## ☕ Support the Project

If APK Signer Pro is useful to you, you can support its continued development by buying me a coffee.

<div align="center">

<a href="https://www.patreon.com/ameermuawiyapk/posts/buy-me-coffee-16891048">
  <img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FF424D?style=for-the-badge&logo=patreon&logoColor=white" alt="Buy Me a Coffee"/>
</a>

<br/>

<a href="https://t.me/itx_muawiya">
  <img src="https://img.shields.io/badge/Telegram-@itx__muawiya-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram"/>
</a>

</div>

---

## 🙏 Acknowledgements

Special thanks to the **ACS Lite team** for making Android development possible directly on mobile.

This project was developed using ACS Lite, allowing the entire application to be built on an Android device without a computer.

<div align="center">

<a href="https://github.com/AndroidCSIDE/ACSIDE">
  <img src="https://img.shields.io/badge/ACS%20Lite-GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="ACS Lite GitHub"/>
</a>

<br/>

<a href="https://t.me/androidcodestudio">
  <img src="https://img.shields.io/badge/ACS%20Lite-Telegram-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="ACS Lite Telegram"/>
</a>

</div>

---

## 📄 License

This project is licensed under the Apache 2.0 License.
