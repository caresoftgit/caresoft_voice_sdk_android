# Third-party components

The Caresoft source in this delivery does not grant rights to unrelated third-party SDKs. Dependencies remain separate Maven artifacts with their own licenses and notices. Preserve upstream licenses/notices when redistributing their binaries. Pinned dependency versions and applicable notices are listed below.

- Jitsi WebRTC Android 124.0.0: https://github.com/jitsi/webrtc ; upstream https://webrtc.googlesource.com/src/ . WebRTC uses BSD-style licensing and includes multiple third-party components. The published Jitsi Maven POM declares Apache License2.0 and MIT. Consult the exact upstream build's LICENSE/PATENTS/third-party notices before production redistribution; POM labels alone are not a complete native-code notice inventory.
- libre4.0.0, commit `09bb0afdfaf1aeac21c0979f3857617a88307f3a`: https://github.com/baresip/re — BSD-3-Clause. [License](licenses/libre-LICENSE.txt). This distribution includes Caresoft modifications; preserve the upstream notice.
- Mbed TLS3.6.5, commit `e185d7fd85499c8ce5ca2a54f5cf8fe7dbe3f8df`: https://github.com/Mbed-TLS/mbedtls — used under Apache-2.0, from the upstream Apache-2.0 OR GPL-2.0-or-later dual license. [Full upstream license and choice](licenses/MbedTLS-LICENSE.txt).
- OkHttp4.9.2 and Okio2.8.0: https://github.com/square/okhttp and https://github.com/square/okio — Apache License2.0.
- Socket.IO Java client2.1.1 and Engine.IO Java client2.1.0: https://github.com/socketio/socket.io-client-java and https://github.com/socketio/engine.io-client-java — MIT.
- Kotlin2.0.21 and kotlinx.coroutines1.7.3: https://github.com/JetBrains/kotlin and https://github.com/Kotlin/kotlinx.coroutines — Apache License2.0.
- Firebase Messaging24.1.0 (demo only): https://firebase.google.com/terms and https://firebase.google.com/support/release-notes/android — host must accept applicable SDK terms and retain bundled notices.
- JUnit4.13.2 (test only): https://github.com/junit-team/junit4 — EPL1.0.
- JSON-java20240303 (JVM test only): https://github.com/stleary/JSON-java — public-domain source statement of that project.
- Gradle8.10.2 wrapper: https://github.com/gradle/gradle — Apache License2.0.

Preserve these notices and the attached license files with redistributed binaries. This package does not certify production acceptance or completion of license clearance.
