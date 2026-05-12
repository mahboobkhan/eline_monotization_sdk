# Eline AdMob Next-Gen SDK

A powerful, production-ready Android Ad SDK built on top of the Google Mobile Ads (GMA) Next-Gen framework. This SDK simplifies ad management with built-in GDPR consent, threshold counters, interval timers, and automatic reloading.

## 🚀 Features
- **Next-Gen Ready**: Built for GMA SDK v1.0.1+.
- **GDPR Compliance**: Built-in UMP Consent Management with EEA detection.
- **Smart Interstitials**: Counter-based and Interval-based display logic.
- **Auto-Reload**: Automatically preloads the next ad after dismissal.
- **Universal Listener**: Standardized callbacks across all ad formats.
- **Pro User Support**: Easy suppression of ads for premium users.

## 📦 Installation

### 1. Add JitPack to your project
In your `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency
In your app `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.YOUR_GITHUB_USERNAME:LBSAdsSDK:1.0.0")
}
```

## 🛠️ Setup

### 1. Update AndroidManifest.xml
Add your AdMob App ID:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>
```

### 2. Initialize the SDK
Initialize in your `SplashActivity`:
```kotlin
NextGenAdsManager.initialize(
    context = this,
    appId = "YOUR_APP_ID",
    billing = billingManager
) {
    // SDK Initialized
    NextGenConsentManager.gatherConsent(this, debug = BuildConfig.DEBUG) { resolved ->
        if (resolved) {
            // Start preloading and loading ads
        }
    }
}
```

## 📺 Usage Examples

### Interstitial (Counter-based)
```kotlin
val manager = NextGenAdsManager.getInterstitialCounterManager(adUnitId = "AD_UNIT_ID")
manager.startPreloading()

// Show when ready (checks counter threshold automatically)
manager.showAd(this) {
    // On dismissed or skipped
}
```

### Banner Ad
```kotlin
NextGenAdsManager.getBannerManager().loadAndShowBanner(
    activity = this,
    container = binding.bannerContainer
)
```

### Native Ad
```kotlin
NextGenAdsManager.getNativeAdManager().loadAndShowNativeAd(
    activity = this,
    container = binding.nativeContainer,
    size = NextGenNativeAdManager.NativeSize.MEDIUM
)
```

## 📄 License
MIT License - Copyright (c) 2026 Eline
