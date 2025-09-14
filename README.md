# Call-Sync 📞

[![Android](https://img.shields.io/badge/Android-6.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A powerful Android library for automatic call log monitoring and synchronization. Call-Sync provides real-time call tracking, local storage, and seamless server synchronization for business applications, fleet management, and call analytics.

## ✨ Features

- 🔄 **Real-time Call Monitoring** - Automatically detects incoming, outgoing, missed, and rejected calls
- 📱 **Multi-SIM Support** - Handles devices with multiple SIM cards
- 💾 **Local Storage** - Room database for offline call log storage
- 🌐 **Server Synchronization** - RESTful API integration with JWT authentication
- 🔒 **Background Processing** - Persistent foreground service with WorkManager
- 🛡️ **Security** - Encrypted local storage and secure API communication
- ⚡ **Performance Optimized** - Debouncing mechanism and efficient resource management
- 🔄 **Auto-restart** - Boot receiver ensures continuous monitoring

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MainActivity  │    │   CallLogService │    │  CallLogObserver│
│                 │    │  (Foreground)    │    │ (ContentObserver)│
└─────────┬───────┘    └─────────┬────────┘    └─────────┬───────┘
          │                      │                       │
          │                      │                       │
          ▼                      ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MainClass     │    │   Room Database  │    │    ApiTask      │
│ (Orchestrator)  │    │  (Local Storage) │    │ (Network Layer) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 🚀 Quick Start

### 1. Add Dependency

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.hk1089:Call-Sync:Tag")
}
```

### 2. Add Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. Initialize Call-Sync

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mainClass = MainClass(this)
        
        // Configure Call-Sync
        val config = HashMap<String, Any>()
        config["sim_slot_index"] = "1"  // SIM slot to monitor
        config["aduserid"] = 419        // User ID
        config["isSimSlot"] = "4"       // SIM identifier
        config["URL_CL"] = "https://your-api.com/call-logs"  // API endpoint
        config["LAST_LOG_TIME"] = ""    // Last sync time (empty for 7 days ago)
        config["is_cache_clear"] = false
        config["isDashboard"] = false
        config["trackCallLog"] = true
        
        // Set API headers
        val headerMap = HashMap<String, Any>()
        headerMap["Content-Type"] = "application/json"
        headerMap["entrymode"] = "1"
        headerMap["authkey"] = "your-jwt-token"
        config["headers"] = headerMap
        
        // Initialize and start monitoring
        mainClass.initializeValue(config)
    }
}
```

## 📋 Configuration Options

| Parameter | Type | Description |
|-----------|------|-------------|
| `sim_slot_index` | String | SIM slot index to monitor (1 or 2) |
| `aduserid` | Int | User/Admin ID for API authentication |
| `isSimSlot` | String | SIM identifier for filtering calls |
| `URL_CL` | String | API endpoint for call log synchronization |
| `LAST_LOG_TIME` | String | Last synchronization timestamp |
| `is_cache_clear` | Boolean | Clear local cache on initialization |
| `isDashboard` | Boolean | Dashboard mode configuration |
| `trackCallLog` | Boolean | Enable/disable call tracking |

## 🔧 API Integration

### Call Log Data Structure

```json
{
  "isUtc": true,
  "simSlotNumber": "1",
  "mobileNo": "4",
  "aduserid": "419",
  "entrymode": "1",
  "callInfo": "[{\"callerID\":\"0\",\"name\":\"John Doe\",\"number\":\"+1234567890\",\"datetime\":\"2024-01-15T10:30:00Z\",\"duration\":\"120\",\"type\":\"Outgoing\"}]"
}
```

### Expected API Response

```json
{
  "success": true,
  "message": "Call logs synchronized successfully"
}
```

## 🛠️ Advanced Usage

### Custom Call Processing

```kotlin
// Get SIM information
mainClass.getSim { simData ->
    // Process SIM data
    Log.d("CallSync", "Available SIMs: $simData")
}

// Check permissions
mainClass.checkPermissions { granted ->
    if (granted) {
        // Permissions granted, start monitoring
        mainClass.doTask()
    } else {
        // Handle permission denial
    }
}

// Manual log synchronization
mainClass.sendLogs()
```

### Service Management

```kotlin
// Stop the monitoring service
mainClass.stopService(context)

// Check if service is running
val isRunning = CallLogService.isServiceRunning()
```

## 📊 Call Data Model

```kotlin
data class CallData(
    val id: Int? = null,
    val callerID: String = "",
    val name: String = "",
    val datetime: String = "",
    val duration: String = "",
    val number: String = "",
    val type: String = "",  // Incoming, Outgoing, Missed, Rejected
    val isSent: Boolean = false,
    val timeMilli: Long = 0L,
    val errorResponse: String = ""
)
```

## 🔒 Security Features

- **JWT Authentication** - Secure API communication
- **Local Encryption** - Room database with encryption
- **Permission Management** - Runtime permission handling
- **Data Validation** - Input sanitization and validation
- **Secure Storage** - Encrypted shared preferences

## 📱 Supported Call Types

- 📞 **Incoming** - Received calls
- 📤 **Outgoing** - Made calls
- ❌ **Missed** - Unanswered incoming calls
- 🚫 **Rejected** - Declined calls
- 📶 **VoWiFi** - Voice over WiFi calls

## 🎯 Use Cases

- **Business Call Tracking** - Monitor employee call activities
- **Fleet Management** - Track driver communications
- **Customer Service** - Analyze customer interactions
- **Compliance** - Maintain call records for regulations
- **Analytics** - Call pattern analysis and reporting

## 🔧 Dependencies

- **AndroidX Core** - Core Android functionality
- **Room** - Local database management
- **WorkManager** - Background task scheduling
- **Fast Android Networking** - HTTP client
- **Toothpick** - Dependency injection
- **Timber** - Logging framework
- **PermissionX** - Runtime permissions

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Contact: [Your Contact Information]

## 🔄 Version History

- **v1.0.0** - Initial release with core call monitoring functionality
- **v1.1.0** - Added multi-SIM support and improved error handling
- **v1.2.0** - Enhanced security and performance optimizations

---

Made with ❤️ for Android developers
