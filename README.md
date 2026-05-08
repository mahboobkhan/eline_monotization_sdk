# LBS Ads SDK 🚀

A comprehensive, modular, and premium Android Ad SDK wrapper for AdMob, Firebase, In-App Billing, and Play Store utilities. Designed for high-performance and leak-free implementation.

---

## 🛠 Features implemented

- [x] **AdMob Suite**:
    - [x] Banner Ads (with Shimmer support)
    - [x] Interstitial Ads (with Loading Dialog)
    - [x] Rewarded Ads (with Loading Dialog)
    - [x] App Open Ads (Lifecycle aware)
    - [x] Native Ads (Customizable Layouts + Shimmer)
- [x] **Analytics & Events**:
    - [x] Firebase Analytics integration
    - [x] Mixpanel Events support
    - [x] Unified `AnalyticsManager` for simultaneous logging
- [x] **In-App Billing (Google Play)**:
    - [x] Subscription Support
    - [x] One-time Product Purchases
    - [x] Restore Purchases
    - [x] Consume Product functionality
- [x] **Remote Management**:
    - [x] Firebase Remote Config for dynamic ad toggling & IDs
- [x] **Play Store Utilities**:
    - [x] In-App Review Manager
    - [x] In-App Update (Flexible & Force)
- [x] **Firebase Cloud Messaging (FCM)**:
    - [x] Custom Messaging Service
    - [x] Custom Notification Design (Day/Night compatible)
    - [x] Permission Handling (Android 13+)
    - [x] Feature Image & Icon Support
    - [x] Topic Subscribe/Unsubscribe
- [x] **Performance & Stability**:
    - [x] Firebase Crashlytics & Performance monitoring
    - [x] Memory Leak Protection (WeakReferences & Application Context)
    - [x] Internet connectivity checks for ad requests

---

## 🚀 Basic Implementation

### 1. Initialization
Initialize the SDK in your `Application` class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Core SDK
        LBSAdsSDK.init(this, "YOUR_MIXPANEL_TOKEN")
        
        // Initialize Ads with Remote Config
        AdsManager.init(this) { success ->
            if (success) {
                // Preload ads if needed
                AdsManager.loadInterstitialAd(this)
            }
        }
    }
}
```

### 2. Loading & Showing Ads

#### Interstitial Ads
```kotlin
// Load
AdsManager.loadInterstitialAd(activity)

// Show
AdsManager.showInterstitialAd(activity) {
    // Callback when ad dismissed or fails
}
```

#### Native Ads
```kotlin
AdsManager.loadNativeAd(
    activity, 
    binding.nativeAdContainer, 
    R.layout.layout_native_ad // Your custom layout
)
```

### 3. In-App Purchases
```kotlin
// Check Pro Status
val isPro = AdsManager.getBillingManager().isUserPro()

// Purchase
AdsManager.getBillingManager().purchaseProduct(
    activity, 
    "your_product_id", 
    BillingClient.ProductType.SUBS
) { success ->
    if (success) { /* Handle success */ }
}
```

### 4. Logging Events
```kotlin
AnalyticsManager.logEvent("button_clicked", Bundle().apply {
    putString("button_name", "home_start")
})
```

### 5. Notifications & FCM

#### Request Permissions
```kotlin
FCMManager.requestNotificationPermission(activity, 101)
```

#### Subscribe to Topics
```kotlin
FCMManager.subscribeToTopic("new_deals") { success ->
    // Handle result
}
```

#### Custom Notification Payload
The SDK handles incoming data payloads automatically. Send these keys via FCM:
- `title`: Notification Title
- `body`: Notification Body
- `icon_url`: Small Icon URL
- `feature_image_url`: Big Feature Image URL
- `is_ad`: Set to "true" to show an AD badge
- `link`: URL to open on click

---

## 📂 Project Structure

- `LBSAdsSDK.kt`: Main entry point and initialization logic.
- `ads/`: Contains managers for all AdMob formats.
- `billing/`: Google Play Billing implementation.
- `remote/`: Firebase Remote Config management.
- `analytics/`: Unified logging for Firebase and Mixpanel.
- `updates/`: Play Store update and review logic.
- `utils/`: UI components (Shimmer, Loading Dialog) and network helpers.

---

## ⚠️ Memory Leak Protection
The SDK uses `WeakReference` for Activity and Mixpanel tracking to ensure that long-lived singleton managers do not prevent the Garbage Collector from reclaiming memory. All managers use `applicationContext` internally for safe, leak-free operation.

---

## 📄 License
Created by **LowByte Studio**. For internal use.
