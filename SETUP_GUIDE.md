# MediWings - Quick Setup & Build Guide

## Prerequisites
- Android Studio (latest version recommended)
- JDK 8 or higher
- Android SDK (API 24-34)
- Firebase account with project set up

## 🔥 IMPORTANT: Firebase Configuration Required

**⚠️ This app CANNOT build or run without proper Firebase configuration!**

The `google-services.json` file is **NOT** included in this repository for security reasons. You **MUST** download it from Firebase Console before building.

### Production vs Development
- **Production**: Use the official "Mediwingsapp" Firebase project
- **Development/Testing**: Create your own Firebase project or ask for access to a test environment

## Initial Setup

### 1. Clone the Repository
```bash
git clone https://github.com/mediwingsoverseas-cmd/MediWings.git
cd MediWings
```

### 2. Firebase Configuration (CRITICAL - Required for Build)

#### Step-by-Step Firebase Setup

**⚠️ IMPORTANT**: The app will NOT build without proper Firebase configuration!

1. **Access Firebase Console**
   - Navigate to [Firebase Console](https://console.firebase.google.com/)
   - Sign in with your Google account

2. **Select/Create the MediWings Firebase Project**
   - Select the **"Mediwingsapp"** project (or create a new project for development/testing)
   - Note: Production uses the official Mediwingsapp project

3. **Configure Android App in Firebase**
   - In Firebase Console, go to **Project Settings** (⚙️ gear icon)
   - Scroll to **"Your apps"** section
   - If Android app exists with package `com.tripplanner.mediwings`:
     - Click on the app
     - Click **"Download google-services.json"**
   - If Android app doesn't exist:
     - Click **"Add app"** → Select Android icon
     - Enter package name: **`com.tripplanner.mediwings`** (EXACTLY as shown)
     - Enter app nickname: "MediWings" (optional)
     - Click **"Register app"**
     - Download the **google-services.json** file

4. **Place google-services.json in Correct Location**
   ```
   MediWings/
   └── app/
       └── google-services.json  ← MUST be exactly here
   ```
   - Copy the downloaded file to `app/google-services.json`
   - **DO NOT** rename the file
   - **DO NOT** commit this file to Git (it's in .gitignore)

5. **Verify google-services.json Content**
   - Open the file and verify:
     - `"package_name": "com.tripplanner.mediwings"` is present
     - `project_id`, `project_number`, and `api_key` are filled in
   - If package name doesn't match, you downloaded the wrong file!

6. **Enable Required Firebase Services**
   
   In your Firebase project, enable these services:
   
   a. **Authentication** (Required)
      - Go to **Build → Authentication**
      - Click **"Get Started"**
      - Enable **"Email/Password"** sign-in method
      - Click "Save"
   
   b. **Realtime Database** (Required)
      - Go to **Build → Realtime Database**
      - Click **"Create Database"**
      - Choose location (e.g., us-central1)
      - Start in **test mode** (for development)
      - Click "Enable"
   
   c. **Cloud Storage** (Required)
      - Go to **Build → Storage**
      - Click **"Get Started"**
      - Start in **test mode** (for development)
      - Click "Done"
   
   d. **Cloud Messaging** (Required)
      - Go to **Build → Cloud Messaging**
      - Cloud Messaging is automatically enabled with your Firebase project
      - No additional configuration needed at this stage
   
   e. **Analytics** (Recommended)
      - Automatically enabled with Firebase
      - View insights in **Analytics** dashboard

7. **Configure Firebase Security Rules (Development)**
   
   For development/testing, use these rules:
   
   **Realtime Database Rules**:
   ```json
   {
     "rules": {
       "users": {
         "$uid": {
           ".read": "auth != null",
           ".write": "auth != null && auth.uid == $uid"
         }
       },
       "Chats": {
         "$chatId": {
           ".read": "auth != null",
           ".write": "auth != null"
         }
       },
       "NotificationQueue": {
         ".read": "auth != null",
         ".write": "auth != null"
       },
       "CMS": {
         ".read": "true",
         ".write": "auth != null"
       },
       "Banners": {
         ".read": "true",
         ".write": "auth != null"
       },
       "Universities": {
         ".read": "true",
         ".write": "auth != null"
       }
     }
   }
   ```
   
   **Storage Rules**:
   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /{allPaths=**} {
         allow read: if request.auth != null;
         allow write: if request.auth != null 
                      && request.resource.size < 1 * 1024 * 1024; // 1MB limit
       }
     }
   }
   ```

### 3. Open in Android Studio
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned MediWings directory
4. Wait for Gradle sync to complete

### 4. Build the Project
```bash
./gradlew assembleDebug
```
Or use Android Studio:
- Build → Make Project (Ctrl+F9)
- Build → Build Bundle(s) / APK(s) → Build APK(s)

### 5. Run on Device/Emulator
```bash
./gradlew installDebug
```
Or use Android Studio:
- Run → Run 'app' (Shift+F10)

## Testing the New Features

### Navigation Test
1. Login as a student
2. Open side menu (hamburger icon)
3. Click "Home" - should navigate to StudentHomeActivity
4. Verify all menu items work

### Chat Test
1. **As Student**:
   - Navigate to Chat from menu
   - Send a text message
   - Click attach button (📎)
   - Select an image (<1MB)
   - Verify image appears in chat

2. **As Admin**:
   - Login as admin (javeedzoj@gmail.com / javeedJaV)
   - Go to User List
   - Click on a student
   - Open chat
   - Send a message
   - Verify student receives notification

### Photo Size Validation Test
1. Try uploading image >1MB
2. Should see error: "Image too large! Please select an image smaller than 1MB (XXXXkB selected)"
3. Try uploading image <1MB
4. Should upload successfully

### Document Preview Test
1. Upload documents (Photos, Aadhar, Passport, HIV)
2. Navigate away and back to Docs tab
3. Verify:
   - Uploaded documents show thumbnails
   - Buttons show "✓ Uploaded - Tap to Replace"
   - Buttons are green

### CMS Content Test
1. As Admin, edit home content in Admin Dashboard
2. As Student, view Home tab
3. Verify content displays with:
   - Modern styling
   - Proper colors (navy + gold)
   - Card background with elevation
   - Good typography and spacing

## Troubleshooting

### Firebase Configuration Issues

**Problem**: "File google-services.json is missing"  
**Symptoms**: Build fails with error about missing google-services.json  
**Solution**: 
- Download `google-services.json` from Firebase Console
- Place it exactly in `app/google-services.json` (not in subdirectories)
- Sync Gradle: File → Sync Project with Gradle Files
- Rebuild: Build → Clean Project, then Build → Rebuild Project

**Problem**: "No matching client found for package name"  
**Symptoms**: Firebase services not working, authentication fails  
**Solution**:
- Open `app/google-services.json`
- Verify `"package_name": "com.tripplanner.mediwings"` is present
- If different, you downloaded the wrong file or configured wrong package in Firebase
- Re-download the correct file from Firebase Console

**Problem**: "Default FirebaseApp is not initialized"  
**Symptoms**: App crashes on launch with Firebase error  
**Solution**:
- Ensure `google-services.json` is in `app/` directory
- Verify the file is valid JSON (not corrupted)
- Check that `id("com.google.gms.google-services")` plugin is applied in `app/build.gradle.kts`
- Clean and rebuild the project

**Problem**: "API key not valid"  
**Symptoms**: Firebase services return authentication errors  
**Solution**:
- Verify API key restrictions in Firebase Console → Project Settings → API Keys
- For development, temporarily remove restrictions
- Ensure the API key in google-services.json matches Firebase Console

### Build Failures

**Problem**: "Could not resolve firebase-bom" or Firebase dependencies  
**Solution**: 
- Check internet connection
- Verify `google-services.json` is in `app/` directory
- Run `./gradlew clean build`
- Check if Maven/Google repositories are accessible
- Try invalidating caches: File → Invalidate Caches / Restart

**Problem**: "Duplicate class" errors  
**Solution**:
- Build → Clean Project
- Build → Rebuild Project
- File → Invalidate Caches & Restart
- Check for conflicting Firebase dependency versions

**Problem**: "Gradle sync failed: Plugin not found"  
**Solution**:
- Ensure internet connection is stable
- Check `build.gradle.kts` has correct plugin versions
- Try updating Gradle: ./gradlew wrapper --gradle-version 8.5

**Problem**: Build fails with "Execution failed for task ':app:processDebugGoogleServices'"  
**Symptoms**: Error about google-services.json processing  
**Solution**:
- Validate google-services.json syntax (use JSON validator)
- Ensure package name matches exactly: `com.tripplanner.mediwings`
- Re-download file from Firebase Console
- Check file permissions (should be readable)

### Runtime Issues

**Problem**: FCM notifications not received  
**Solution**:
- Test on physical device (emulator has FCM limitations)
- Check Firebase Console → Cloud Messaging (service enabled)
- Verify POST_NOTIFICATIONS permission granted (Android 13+)
- Check device notification settings for the app
- Verify FCM token is being generated (check Logcat for "FCM_TOKEN")
- Ensure app is not in battery optimization/doze mode

**Problem**: Images not loading  
**Solution**:
- Verify internet connection
- Check Firebase Storage rules (should allow read/write for authenticated users)
- Ensure READ_MEDIA_IMAGES permission granted (Android 13+)
- Check Firebase Storage bucket URL in console
- Verify storage usage hasn't exceeded free tier limits

**Problem**: Chat not working / Messages not sending  
**Solution**:
- Verify Firebase Realtime Database rules allow read/write
- Check authentication state (user must be logged in)
- Look for errors in Logcat: `adb logcat | grep -E "Firebase|Chat"`
- Ensure internet connectivity
- Check database URL matches in Firebase Console

**Problem**: Authentication fails  
**Solution**:
- Verify Email/Password provider is enabled in Firebase Console
- Check user credentials are correct
- Look for authentication errors in Logcat
- Ensure google-services.json is correctly configured
- Try disabling and re-enabling Email/Password auth in Console

**Problem**: "Permission denied" errors from Firebase  
**Solution**:
- Check Firebase Realtime Database rules
- Check Firebase Storage rules
- Ensure user is authenticated before accessing protected resources
- For development, temporarily use test mode rules (allow all)
- Review rules in Firebase Console → Build → Database/Storage → Rules

## Firebase Console Setup

### Realtime Database Rules (Development)
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "Chats": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "NotificationQueue": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "CMS": {
      ".read": "true",
      ".write": "auth != null"
    },
    "Banners": {
      ".read": "true",
      ".write": "auth != null"
    },
    "Universities": {
      ".read": "true",
      ".write": "auth != null"
    }
  }
}
```

### Storage Rules (Development)
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null 
                   && request.resource.size < 1 * 1024 * 1024; // 1MB limit
    }
  }
}
```

## Admin Login Credentials
- **Email**: javeedzoj@gmail.com
- **Password**: javeedJaV
- Hardcoded in `MainActivity.kt`

## Student Registration
1. Click "Register" on login screen
2. Enter details:
   - Name
   - Email
   - Mobile
   - Password
3. Login with created credentials

## Key Files Reference

### Activities
- `MainActivity.kt` - Login screen
- `StudentHomeActivity.kt` - Student main screen (tabs)
- `StudentActivity.kt` - Old student screen (viewpager)
- `ChatActivity.kt` - Chat interface
- `AdminDashboardActivity.kt` - Admin panel
- `AdminBannerManagementActivity.kt` - Banner upload
- `AdminAddUniversityActivity.kt` - University management

### Services
- `MyFirebaseMessagingService.kt` - FCM notification handler

### Layouts
- `activity_studend.xml` - Student home (note: typo in filename)
- `activity_chat.xml` - Chat UI
- `item_message_in.xml` - Incoming message bubble
- `item_message_out.xml` - Outgoing message bubble

### Configuration
- `build.gradle.kts` - Dependencies
- `AndroidManifest.xml` - Permissions, activities, services

## Debugging Tips

### Enable Verbose Logging
Add to `onCreate()`:
```kotlin
FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
```

### View Logcat
```bash
adb logcat | grep -E "MediWings|Firebase|FCM"
```

### Check FCM Token
In `StudentHomeActivity.onCreate()`:
```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    Log.d("FCM_TOKEN", "Token: $token")
}
```

## Performance Optimization

### Image Loading
- Glide automatically caches images
- Consider adding placeholder images
- Use `.thumbnail(0.1f)` for progressive loading

### Database Queries
- Use `.limitToLast(50)` for message queries
- Remove ValueEventListeners in `onDestroy()`
- Consider pagination for large datasets

## Security Considerations

### Before Production
1. **Update Firebase Rules**: Restrict write access properly
2. **Remove Hardcoded Credentials**: Move admin login to Firebase
3. **Add ProGuard Rules**: Obfuscate code
4. **Enable App Signing**: Use Android App Bundle
5. **Validate All Inputs**: Server-side validation recommended

### Recommended Security Rules (Production)
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth.uid == $uid",
        ".write": "auth.uid == $uid",
        "fcmToken": {
          ".write": "auth.uid == $uid"
        }
      }
    },
    "Chats": {
      "$chatId": {
        ".read": "auth.uid == $chatId || auth.uid == 'ADMIN_UID'",
        ".write": "auth.uid == $chatId || auth.uid == 'ADMIN_UID'"
      }
    }
  }
}
```

## 🚀 Production Deployment Checklist

### Pre-Production Requirements

Before deploying to production, ensure all of the following are complete:

#### 1. Firebase Configuration ✅
- [x] Application ID set to `com.tripplanner.mediwings` 
- [x] google-services.json configured for production Firebase project
- [x] Firebase BoM updated to latest stable version (33.7.0)
- [x] All required Firebase services enabled (Auth, Database, Storage, Messaging, Analytics)
- [ ] Production Firebase Security Rules implemented (see below)
- [ ] Firebase project billing enabled for production scale

#### 2. Security Configuration
- [ ] Update Firebase Database rules to production-ready settings
- [ ] Update Firebase Storage rules to production-ready settings  
- [ ] Remove hardcoded admin credentials (move to Firebase Auth admin claims)
- [ ] Enable ProGuard/R8 code obfuscation (set `isMinifyEnabled = true` in release build)
- [ ] Add ProGuard rules to keep necessary classes (already configured in proguard-rules.pro)
- [ ] Review and secure all API endpoints
- [ ] Implement rate limiting on sensitive operations

#### 3. Build Configuration
- [ ] Generate signed release APK/AAB with production keystore
- [ ] Update versionCode and versionName for release
- [ ] Test release build thoroughly (different from debug build)
- [ ] Enable App Signing in Google Play Console
- [ ] Configure build variants for different environments if needed

#### 4. Testing & Quality Assurance
- [ ] Complete end-to-end testing on physical devices
- [ ] Test on multiple Android versions (API 24-34)
- [ ] Test on different screen sizes and densities
- [ ] Verify FCM notifications work in production
- [ ] Load testing for database and storage
- [ ] Security audit of Firebase rules
- [ ] Test offline scenarios and error handling

#### 5. Documentation & Monitoring
- [ ] Document deployment procedures
- [ ] Set up Firebase Crashlytics for crash reporting
- [ ] Configure Firebase Performance Monitoring
- [ ] Set up alerts for critical Firebase events
- [ ] Document rollback procedures

### Production Firebase Security Rules

**⚠️ IMPORTANT**: Replace test mode rules with these production rules before launch!

**Realtime Database (Production)**:
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('users').child(auth.uid).child('isAdmin').val() === true",
        ".write": "auth.uid == $uid",
        "fcmToken": {
          ".write": "auth.uid == $uid"
        }
      }
    },
    "Chats": {
      "$chatId": {
        ".read": "auth != null && (auth.uid == $chatId || root.child('users').child(auth.uid).child('isAdmin').val() === true)",
        ".write": "auth != null && (auth.uid == $chatId || root.child('users').child(auth.uid).child('isAdmin').val() === true)"
      }
    },
    "NotificationQueue": {
      ".read": "root.child('users').child(auth.uid).child('isAdmin').val() === true",
      ".write": "root.child('users').child(auth.uid).child('isAdmin').val() === true"
    },
    "CMS": {
      ".read": "true",
      ".write": "root.child('users').child(auth.uid).child('isAdmin').val() === true"
    },
    "Banners": {
      ".read": "true",
      ".write": "root.child('users').child(auth.uid).child('isAdmin').val() === true"
    },
    "Universities": {
      ".read": "true",
      ".write": "root.child('users').child(auth.uid).child('isAdmin').val() === true"
    }
  }
}
```

**Storage (Production)**:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // User profile images
    match /users/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null 
                   && request.auth.uid == userId
                   && request.resource.size < 1 * 1024 * 1024  // 1MB limit
                   && request.resource.contentType.matches('image/.*');
    }
    
    // Chat images
    match /chat_images/{chatId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
                   && request.resource.size < 1 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
    
    // Admin uploaded content (banners, etc)
    match /banners/{fileName} {
      allow read: if true;
      allow write: if request.auth != null; // Add isAdmin check
    }
  }
}
```

### Post-Deployment Steps
1. Monitor Firebase Console for errors and unusual activity
2. Check Firebase Analytics for user behavior insights  
3. Monitor Crashlytics for any crash reports
4. Verify FCM notifications are being delivered
5. Check database and storage usage against quotas
6. Monitor API quota usage
7. Set up automated backup procedures for critical data

## Next Steps

### Immediate
- [ ] Test all features on physical device
- [ ] Verify notifications work end-to-end
- [ ] Check photo size limits across all screens
- [ ] Test on different screen sizes

### Near Future
- [ ] Implement Cloud Function for FCM notifications
- [ ] Add message deletion/editing
- [ ] Support more file types (PDF, DOC)
- [ ] Add message search functionality
- [ ] Implement user blocking/reporting

### Long Term
- [ ] Add video call feature
- [ ] Multi-language support
- [ ] Offline mode with sync
- [ ] Analytics integration
- [ ] Advanced admin dashboard

## Need Help?

### Resources
- [Firebase Documentation](https://firebase.google.com/docs)
- [Android Developer Guide](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs)

### Common Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test

# Check for updates
./gradlew dependencyUpdates
```

---

**Happy Coding! 🚀**
