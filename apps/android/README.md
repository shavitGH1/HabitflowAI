# HabitFlow AI Android POC

## Run in Android Studio
1. Open the `Frontend` folder as an Android project.
2. Sync Gradle.
3. Run the `app` configuration on an emulator or device.

## Required environment
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

## Notes
- API base URL is set to `http://10.0.2.2:3000/` for Android emulator testing.
- This is a single-module Compose POC with MVVM + clean architecture boundaries.
- If Gradle sync complains about the wrapper JAR, let Android Studio regenerate the wrapper or add `gradle/wrapper/gradle-wrapper.jar` from a standard Gradle distribution.

