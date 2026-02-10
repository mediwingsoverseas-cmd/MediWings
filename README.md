# MediWings - Medical Travel Planning App

MediWings is an Android application for managing medical travel planning, connecting students/patients with medical service providers overseas.

## 🚀 Quick Start

### Prerequisites
- **Android Studio** Arctic Fox or later
- **JDK 8** or higher
- **Android SDK** with API levels 24-34
- **Firebase Account** with a configured project

### Firebase Setup (REQUIRED)

This app uses Firebase for backend services. Before building, you **MUST** configure Firebase:

#### 1. Create/Access Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select the **Mediwingsapp** project (or create a new one for development)
3. Ensure the project is configured for Android

#### 2. Download google-services.json
1. In Firebase Console, go to **Project Settings** (gear icon)
2. Under **Your apps**, select the Android app with package name `com.tripplanner.mediwings`
   - If not exists, click **Add app** → Android icon
   - Enter package name: `com.tripplanner.mediwings`
   - Register the app
3. Download the `google-services.json` file
4. **IMPORTANT**: Place the downloaded file in `app/google-services.json`
   ```
   MediWings/
   └── app/
       └── google-services.json  ← Place file here
   ```

#### 3. Enable Firebase Services
Enable these services in your Firebase project:
- ✅ **Authentication** (Email/Password provider)
- ✅ **Realtime Database**
- ✅ **Cloud Storage**
- ✅ **Cloud Messaging** (FCM)
- ✅ **Analytics**

### Build & Run

```bash
# Clone the repository
git clone https://github.com/mediwingsoverseas-cmd/MediWings.git
cd MediWings

# Build the project
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

Or use **Android Studio**:
1. Open the project in Android Studio
2. Let Gradle sync complete
3. Click **Run** (▶️) or press **Shift+F10**

## 📋 Application Features

### For Students/Patients
- ✈️ Browse medical programs and universities
- 💬 Real-time chat with support staff
- 📄 Document upload (passport, medical records, etc.)
- 📢 View announcements and banners
- 📱 Push notifications for updates

### For Workers/Support Staff
- 👥 View and manage student profiles
- 💬 Chat support with students
- 📊 Track application progress

### For Administrators
- 👤 User management
- 🏥 University/program management
- 📰 Content Management System (CMS)
- 🎨 Banner management
- 📞 Contact information updates

## 🔧 Firebase Configuration Details

### Package Name
- **Application ID**: `com.tripplanner.mediwings`
- This MUST match the package name in your Firebase project

### Firebase SDK Components
This app uses the following Firebase services:
- **Firebase Analytics**: App usage insights
- **Firebase Authentication**: User authentication
- **Firebase Realtime Database**: Real-time data sync
- **Firebase Cloud Storage**: File/image storage
- **Firebase Cloud Messaging**: Push notifications

### Version Management
The app uses Firebase BoM (Bill of Materials) version **33.7.0** to ensure all Firebase dependencies are compatible.

## 📱 App Configuration

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with ProGuard/R8 optimization

### Minimum Requirements
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34

## 🔐 Security Notes

### Important: google-services.json
- ⚠️ The `google-services.json` file contains sensitive API keys
- ✅ This file is **NOT** committed to the repository (in .gitignore)
- ✅ Each developer must download it from Firebase Console
- ✅ For production, ensure proper Firebase security rules are configured

### Production Checklist
Before deploying to production:
- [ ] Update Firebase Database rules for production
- [ ] Update Firebase Storage rules for production
- [ ] Enable ProGuard/R8 code obfuscation
- [ ] Use signed release build
- [ ] Remove any debug/test code
- [ ] Review and update all API keys

## 📚 Additional Documentation

- [UPGRADE_DOCUMENTATION.md](UPGRADE_DOCUMENTATION.md) - **NEW**: Latest upgrades and improvements (v2.0)
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Detailed setup and testing guide
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Testing procedures
- [WORKER_DOCUMENTATION.md](WORKER_DOCUMENTATION.md) - Worker interface documentation

## 🛠️ Development Tools

### Key Technologies
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **UI Framework**: Android SDK with Material Design
- **Backend**: Firebase (Auth, Database, Storage, Messaging)
- **Image Loading**: Glide
- **Rich Text**: RichEditor-Android

### Project Structure
```
app/src/main/java/com/tripplanner/mediwings/
├── MainActivity.kt                 # Login screen
├── RegisterActivity.kt             # User registration
├── StudentHomeActivity.kt          # Student dashboard
├── WorkerActivity.kt               # Worker interface
├── AdminDashboardActivity.kt       # Admin panel
├── ChatActivity.kt                 # Real-time chat
├── MyFirebaseMessagingService.kt   # FCM handler
└── ... (other activities)
```

## 🔍 Troubleshooting

### Build Errors

**Error**: `File google-services.json is missing`
- **Solution**: Download `google-services.json` from Firebase Console and place in `app/` directory

**Error**: `Package name mismatch`
- **Solution**: Ensure package name in `google-services.json` matches `com.tripplanner.mediwings`

**Error**: `Failed to resolve firebase dependencies`
- **Solution**: Check internet connection and sync Gradle again

### Runtime Issues

**Issue**: Firebase services not working
- Verify `google-services.json` is correctly placed
- Check Firebase project configuration in console
- Ensure internet connectivity

**Issue**: Push notifications not received
- Test on physical device (emulator has limitations)
- Verify Cloud Messaging is enabled in Firebase
- Check notification permissions are granted

## 🤝 Contributing

This is a production application for MediWings Overseas. For contributions:
1. Follow existing code style and conventions
2. Test all changes thoroughly
3. Ensure Firebase configuration is not compromised
4. Document any new features or changes

## 📞 Support

For technical support or questions:
- Review documentation in this repository
- Check Firebase Console for service status
- Contact the development team

## 📄 License

Proprietary - MediWings Overseas
All rights reserved.

---

**Built with ❤️ for better healthcare access worldwide**
