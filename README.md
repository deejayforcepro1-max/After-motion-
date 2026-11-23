# AfterMotion

Motion graphics, keyframe animation, and real-time VFX compositing studio for Android (Jetpack Compose).

## Automated Debug APK & Direct Download (GitHub Actions + Releases)

This project includes `.github/workflows/build-debug-apk.yml`, which automatically builds and publishes **`AfterMotion-debug.apk`** on GitHub.

### How It Works
- **On every push to `main` / `master`**:
  1. Sets up JDK 21, Android SDK, and Gradle `9.3.1`.
  2. Decodes `debug.keystore.base64` to `debug.keystore` for consistent debug signing.
  3. Runs unit tests (`KeyframeEngineTest`) and builds `:app:assembleDebug`.
  4. Uploads `AfterMotion-debug.apk` as a GitHub Actions Artifact.
  5. Creates/updates the `latest-debug` **GitHub Release** with `AfterMotion-debug.apk` attached.
- **On version tags (`v*`, e.g., `v1.0.0`) or manual `workflow_dispatch`**:
  - Publishes a versioned GitHub Release with `AfterMotion-debug.apk` attached.

### Direct APK Download Links
Once pushed to [`deejayforcepro1-max/After-motion-`](https://github.com/deejayforcepro1-max/After-motion-), the latest debug APK can be downloaded directly at:

- **Latest Debug APK (rolling `main` build)**:
  `https://github.com/deejayforcepro1-max/After-motion-/releases/download/latest-debug/AfterMotion-debug.apk`
- **All Releases**:
  `https://github.com/deejayforcepro1-max/After-motion-/releases`
- **Tagged Release APK** (e.g. `v1.0.0`):
  `https://github.com/deejayforcepro1-max/After-motion-/releases/download/v1.0.0/AfterMotion-debug.apk`
