# iOS (not yet)

Everything the app does is in `shared/` (common Kotlin and Compose Multiplatform), so an iOS app only needs
a shell, as `androidApp/` is for Android. It needs a Mac with Xcode:

1. In `shared/build.gradle.kts`, uncomment the iOS targets (`iosArm64()`, `iosSimulatorArm64()`): they build
   a `Shared` framework.
2. In `shared/src/iosMain/kotlin/.../ui/`, add the iOS `PlatformBackHandler` (an empty one: iOS has no back
   button) and a `MainViewController()` that returns `ComposeUIViewController { App(defaultColors(dark)) }`.
3. Create the Xcode project here (the Kotlin Multiplatform wizard at https://kmp.jetbrains.com makes one
   to copy), with a SwiftUI view that shows `MainViewController()`.
