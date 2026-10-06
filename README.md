<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="140" alt="APK Signer Pro Icon"/>

# APK Signer Pro

### Sign Android APKs Easily & Securely

<img src="https://i.ibb.co/67chvKHX/upload-1791255917747.png" width="100%" alt="APK Signer Pro"/>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat)](LICENSE)

</div>

APK Signer Pro is an open-source Android application for signing, verifying, extracting, backing up, and managing APK, APKS, and XAPK packages directly on your Android device.

Built with Kotlin and Jetpack Compose, it combines advanced APK signing capabilities with a clean Material 3 Expressive interface designed for developers and Android power users.

---

## ✨ Key Features

- **v1 APK Signing**: Legacy JAR-based APK signing for older Android versions.
- **v2 APK Signing & Verification**: Full-file binary integrity protection for Android 7.0+.
- **v3 APK Signing & Verification**: Key rotation and cryptographic lineage support for Android 9.0+.
- **v4 APK Signing**: Streaming signature generation with `.apk.idsig` support for Android 11.0+.
- **16KB & 4KB Page Alignment**: Aligns native `.so` libraries and `resources.arsc` for modern Android compatibility.
- **Multi-Format Keystores**: Supports `JKS`, `PKCS12`, `BKS`, `BKS-V1`, `UBER`, and `BCFKS`.
- **Built-in Debug Key**: Quickly sign applications using the standard Android Studio debug credentials.
- **Certificate Inspection**: View certificate algorithm, validity, subject, issuer, and SHA-256 fingerprint.
- **Persistent Credentials**: Save key associations for faster signing without repeated prompts.
- **APK Backup & Extraction**: Back up and extract APKs from installed user and system applications.
- **Split APK Support**: Handle split APK packages and `.apks` files.
- **XAPK Support**: Work with XAPK packages alongside standard APK and APKS files.
- **APK Management**: Install, share, copy paths, and remove generated packages.
- **Signing History**: Keep track of previous signing operations.
- **Search & Filtering**: Quickly find records and installed applications.
- **Reactive Local Storage**: SQLite persistence with Kotlin Flow.
- **Organized Storage**: Automatically organize signed packages and backups.
- **Secure File Handling**: Uses Android's `FileProvider` and system sharing mechanisms.

---

## 🎨 Material 3 Expressive UI

- **Material 3 Expressive**: Modern interface built entirely with Jetpack Compose.
- **Dynamic Colors**: Adapts to the system wallpaper color palette.
- **Light & Dark Themes**: Supports system-based theme modes.
- **Expressive Loading Indicators**: Smooth loading indicators across different screens and actions.
- **Pull-to-Refresh**: Expressive swipe-to-refresh interactions.
- **Collapsing Top App Bars**: Smooth nested scrolling across primary screens.
- **Live Logs**: Colorized signing and execution logs with step indicators.
- **One-Tap Log Copying**: Quickly copy logs to the clipboard.
- **Consistent Empty States**: Clean and polished empty-state layouts.
- **Responsive Layouts**: Designed for a consistent experience across Android devices.
- **Multi-language Support**: Available in English, Chinese, Russian, Portuguese, French, Spanish, German, and Arabic.

---

## 📸 Screenshots

<table>
  <tr>
    <td width="50%"><img src="screenshots/1.jpg" alt="APK Signer Pro Home"/></td>
    <td width="50%"><img src="screenshots/2.jpg" alt="APK Signer Pro History"/></td>
  </tr>
  <tr>
    <td width="50%"><img src="screenshots/3.jpg" alt="APK Signer Pro Signing"/></td>
    <td width="50%"><img src="screenshots/4.jpg" alt="APK Signer Pro Keystore"/></td>
  </tr>
  <tr>
    <td width="50%"><img src="screenshots/5.jpg" alt="APK Signer Pro Settings"/></td>
    <td width="50%"><img src="screenshots/6.jpg" alt="APK Signer Pro Logs"/></td>
  </tr>
  <tr>
    <td width="50%"><img src="screenshots/7.jpg" alt="APK Signer Pro Interface"/></td>
    <td width="50%"><img src="screenshots/8.jpg" alt="APK Signer Pro Interface"/></td>
  </tr>
</table>

---

## ☕ Support the Project

If APK Signer Pro is useful to you, you can support its continued development by buying me a coffee.

<div align="center">

<a href="https://www.patreon.com/ameermuawiyapk/posts/buy-me-coffee-16891048">
  <img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FF424D?style=for-the-badge&logo=patreon&logoColor=white" alt="Buy Me a Coffee"/>
</a>

&nbsp;

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

&nbsp;

<a href="https://t.me/androidcodestudio">
  <img src="https://img.shields.io/badge/ACS%20Lite-Telegram-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="ACS Lite Telegram"/>
</a>

</div>

---

## 📄 License

This project is licensed under the Apache 2.0 License.