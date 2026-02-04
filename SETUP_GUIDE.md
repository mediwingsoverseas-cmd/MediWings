# MediWings - Quick Setup & Build Guide

## Prerequisites
- Android Studio (latest version recommended)
- JDK 8 or higher
- Android SDK (API 24-34)
- Firebase account with project set up

## Initial Setup

### 1. Clone the Repository
```bash
git clone https://github.com/mediwingsoverseas-cmd/MediWings.git
cd MediWings
```

### 2. Firebase Configuration
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your MediWings project
3. Download `google-services.json`
4. Place it in `app/` directory
5. Ensure these services are enabled:
   - Authentication (Email/Password)
   - Realtime Database
   - Storage
   - Cloud Messaging

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

### Build Failures

**Problem**: "Could not resolve firebase-messaging"  
**Solution**: 
- Check internet connection
- Verify `google-services.json` is in `app/` directory
- Run `./gradlew clean build`

**Problem**: "Duplicate class" errors  
**Solution**:
- Build → Clean Project
- Build → Rebuild Project
- Invalidate Caches & Restart

### Runtime Issues

**Problem**: FCM notifications not received  
**Solution**:
- Test on physical device (emulator has limitations)
- Check Firebase Console → Cloud Messaging
- Verify POST_NOTIFICATIONS permission granted
- Check device notification settings

**Problem**: Images not loading  
**Solution**:
- Verify internet connection
- Check Firebase Storage rules
- Ensure READ_MEDIA_IMAGES permission granted

**Problem**: Chat not working  
**Solution**:
- Verify Firebase Realtime Database rules
- Check authentication state
- Look for errors in Logcat

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
