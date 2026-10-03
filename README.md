# APK Signer (Material 3 Expressive)

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An open-source, modern Android application designed for signing, verifying, extracting, and managing APK, APKS, and XAPK package files on Android devices with pure Kotlin and Jetpack Compose.

---

## 📸 Screenshots & Architecture Previews

<table>
  <tr>
    <td width="50%"><img src="screenshots/1.jpg"/></td>
    <td width="50%"><img src="screenshots/2.jpg"/></td>
  </tr>
  <tr>
    <td width="50%"><img src="screenshots/3.jpg"/></td>
    <td width="50%"><img src="screenshots/4.jpg"/></td>
  </tr>
  <tr>
    <td width="50%"><img src="screenshots/5.jpg"/></td>
    <td width="50%"><img src="screenshots/6.jpg"/></td>
  </tr>
  <tr>
    <td width="50%"><img src="screenshots/7.jpg"/></td>
    <td width="50%"><img src="screenshots/8.jpg"/></td>
  </tr>
</table>

---

## ✨ Key Features

### 🔐 Multi-Scheme Signing & Integrity Verification
- **v1 Scheme (JAR signing)**: Backward compatible with legacy Android versions (Android 4.4 and lower).
- **v2 Scheme (APK Signature Scheme v2)**: Full-file binary integrity verification (Android 7.0+).
- **v3 Scheme (APK Signature Scheme v3)**: Key rotation and cryptographic lineage support (Android 9.0+).
- **v4 Scheme (APK Signature Scheme v4)**: Streaming signature files (`.apk.idsig`) for fast adb/incremental installations (Android 11.0+).
- **Automated 16KB & 4KB Page Alignment**: Guarantees uncompressed native `.so` libraries and `resources.arsc` are aligned to 16384-byte (16KB Android 15+ standard) and 4-byte boundaries for zero runtime crashes.

### 🔑 Advanced Keystore & Certificate Inspection
- **Multi-Format Keystore Engine**: Full support for `JKS`, `PKCS12`, `BKS`, `BKS-V1`, `UBER`, and `BCFKS` key formats.
- **Built-in Debug Key**: Instant signing with standard Android Studio debug credentials (`androiddebugkey`).
- **Interactive Verification Dialog**: Pre-verifies keystores before saving, displaying certificate algorithm, validity period, subject DN, issuer, and SHA-256 fingerprints in read-only inspector fields.
- **Persistent Credential Association**: Stores passwords securely per key record so signing works seamlessly with one tap without repetitive prompts.
- **Non-Disruptive Keystore Selector**: Bottom sheet allows smooth key switching without accidental dismissals.

### 📦 Application Backup & Split APK Handling
- **Backup & Extraction**: One-tap backup of single APKs and split APK bundles (`.apks`) from user and system apps.
- **Dedicated Directory Organization**: Automatically organizes output into `Downloads/APKSigner/Backup` and `Downloads/APKSigner/Signed`.
- **System Sharing & Installation**: Integrated Android sharesheet and direct package installation via secure `FileProvider`.

### ⚡ Reactive History & Local Persistence
- **High-Performance SQLite Persistence**: Reactive Flow updates with zero main-thread blocking.
- **Search & Filter**: Real-time filtering across signing history and installed apps.
- **File Management**: Quick options to copy file path, install, share, or purge records and files from storage.

### 🎨 Material 3 Expressive Design
- **Grouped Card Geometry**: Smooth dynamic corner radii matching modern Material 3 guidelines.
- **Clipped Ripple Effects**: Contained touch ripples strictly respecting rounded corners.
- **Dynamic Color (Material You)**: Automatic wallpaper accent palette adaptation with Light & Dark themes.
- **Collapsing Top App Bars**: Smooth nested scroll behavior across all primary screens.
- **Live Terminal Log Viewer**: Colorized execution logs with step indicators and one-tap clipboard copying.

---

## 📄 License

This project is licensed under the Apache 2.0 License.
