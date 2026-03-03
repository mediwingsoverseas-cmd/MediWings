# MediWings - Comprehensive Troubleshooting Guide

## 🔍 Overview
This guide provides solutions for common issues encountered while building, running, and using the MediWings application.

---

## 📱 Build & Setup Issues

### Issue: "File google-services.json is missing"
**Symptom**: Build fails with error about missing google-services.json

**Solution**:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your MediWings project
3. Navigate to Project Settings (gear icon)
4. Under "Your apps", download the `google-services.json` file
5. Place it in the `app/` directory (NOT in the root directory)
6. Rebuild the project

**Verification**:
```bash
ls -la app/google-services.json
# Should show the file exists
```

### Issue: "Package name mismatch"
**Symptom**: Firebase authentication or services not working

**Solution**:
1. Verify package name in `google-services.json` matches `com.tripplanner.mediwings`
2. Check `app/build.gradle.kts` applicationId is `com.tripplanner.mediwings`
3. If mismatch exists, download correct google-services.json from Firebase Console

### Issue: "Failed to resolve firebase dependencies"
**Symptom**: Gradle sync fails with Firebase dependency errors

**Solutions**:
1. Check internet connection
2. Update Gradle: `./gradlew wrapper --gradle-version 8.5`
3. Clear Gradle cache:
   ```bash
   ./gradlew clean
   rm -rf ~/.gradle/caches
   ```
4. Sync Gradle again in Android Studio

---

## 🔐 Authentication Issues

### Issue: "Authentication required. Please log in."
**Symptom**: Chat or other features show this error

**Root Cause**: Firebase authentication state lost

**Solution**:
1. Log out completely from the app
2. Close and reopen the app
3. Log in again with correct credentials
4. Verify internet connection is stable

### Issue: "No account found with this email"
**Symptom**: Cannot log in with email

**Solutions**:
1. Verify email is typed correctly (no extra spaces)
2. Check if you registered as Student or Worker (try both)
3. Register a new account if needed
4. Contact admin if account should exist but doesn't

### Issue: "Incorrect password"
**Symptom**: Login fails with password error

**Solutions**:
1. Check Caps Lock is off
2. Verify password is at least 6 characters
3. Try resetting password via Firebase Console (admin access required)
4. Register new account if forgotten

---

## 💬 Chat Issues

### Issue: Chat crashes immediately on open
**Root Causes**: 
- Invalid chat ID
- Missing role parameter
- Network connectivity issues

**Solutions**:
1. Ensure you're logged in with correct role (Student or Worker)
2. Check internet connection
3. Try logging out and back in
4. Clear app data and log in again

### Issue: "Invalid chat session. Please try again."
**Root Cause**: Chat ID generation failed

**Solution**:
1. Go back to home screen
2. Ensure you're properly authenticated
3. Try opening chat again
4. If persists, log out and back in

### Issue: Messages not sending
**Symptom**: Message stays in "sending" state

**Solutions**:
1. Check internet connection
2. Verify Firebase Realtime Database is accessible
3. Check if message is too long (max ~10KB)
4. Try sending a shorter message
5. Check Firebase Console for database rules

### Issue: "Upload failed: [error]"
**Symptom**: Cannot send images in chat

**Solutions**:
1. Check internet connection
2. Verify file is under 1MB (1024KB)
3. Ensure Storage permissions granted
4. Try selecting a different image
5. Check Firebase Storage is properly configured

---

## 📤 Upload Issues

### Issue: "File too large! [size]KB selected, max 1MB"
**Symptom**: Document or image upload rejected

**Solution**:
1. Compress the image using:
   - Online tools: TinyPNG, Squoosh
   - Phone apps: Photo Compress, Reduce Photo Size
2. Ensure image is under 1MB (1024KB)
3. Select a different, smaller file

### Issue: "Invalid file selected. Please try again."
**Symptom**: Upload fails immediately

**Solutions**:
1. Ensure file is a valid image format (JPG, PNG)
2. Try selecting from a different location (Gallery vs Downloads)
3. Check file is not corrupted
4. Restart the app and try again

### Issue: Upload succeeds but preview doesn't show
**Symptom**: Upload completes but no image visible

**Solutions**:
1. Pull down to refresh (if available)
2. Navigate away and back to the screen
3. Check internet connection for loading images
4. Clear app cache in Settings > Apps > MediWings > Clear Cache

---

## 👥 Student/Worker Separation Issues

### Issue: Seeing wrong chat history after switching roles
**Root Cause**: This should NOT happen - chats are role-specific

**Verification**:
1. Student chats use format: `{userId}_student`
2. Worker chats use format: `{userId}_worker`
3. These are completely separate

**If Issue Persists**:
1. Log out completely
2. Clear app data
3. Log in with correct role
4. Report to developers if still occurring

### Issue: Admin can't see student or worker users
**Symptom**: Empty user list in admin panel

**Solutions**:
1. Verify admin mode selected correctly (Student Admin or Worker Admin)
2. Ensure users exist in Firebase Database under `/users`
3. Check users have `role: "student"` or `role: "worker"` field
4. Verify Firebase Database rules allow admin read access

---

## 🔥 Firebase Integration Issues

### Issue: "Failed to initialize Firebase"
**Symptom**: App crashes on startup or navigation

**Solutions**:
1. Verify `google-services.json` is present in `app/` directory
2. Check internet connectivity
3. Verify Firebase project is active and not suspended
4. Rebuild the app: `./gradlew clean assembleDebug`

### Issue: "Failed to load [data]: [error]"
**Symptom**: Data doesn't load from Firebase

**Solutions**:
1. Check internet connection
2. Verify Firebase services are enabled in Console:
   - Realtime Database
   - Authentication
   - Storage
3. Check Firebase Database Rules are correctly configured
4. Verify data exists in Firebase Console

### Issue: Network errors frequently
**Symptom**: Multiple "network error" messages

**Solutions**:
1. Check device WiFi/mobile data
2. Try switching between WiFi and mobile data
3. Check if Firebase is down: [Firebase Status](https://status.firebase.google.com/)
4. Verify no firewall blocking Firebase domains

---

## 🔐 Admin-Specific Issues

### Issue: Admin can't access certain features
**Symptom**: Buttons don't work or screens are empty

**Solutions**:
1. Verify logged in as admin (javeedzoj@gmail.com)
2. Select correct admin mode (Student Admin or Worker Admin)
3. Verify Firebase Database rules allow admin access
4. Check specific feature is implemented for that admin mode

### Issue: "No [students/workers] found"
**Symptom**: User list is empty

**Solutions**:
1. Verify users are registered in the system
2. Check Firebase Database has users under `/users` path
3. Ensure users have correct `role` field ("student" or "worker")
4. Verify admin mode matches the role you're looking for

### Issue: Dashboard statistics show 0
**Symptom**: User count or chat count is zero

**Solutions**:
1. Check Firebase Database has data
2. Verify chat IDs follow format: `{userId}_{role}`
3. Ensure users have correct `role` field
4. Check Firebase Database rules allow read access
5. Try refreshing by reopening admin dashboard

---

## 📱 Permission Issues

### Issue: "Permission denied. Cannot upload images."
**Symptom**: Image picker doesn't open

**Solutions**:
1. Grant storage permissions in device Settings:
   - Android 13+: Photos and media permission
   - Android 12 and below: Storage permission
2. Path: Settings > Apps > MediWings > Permissions
3. Enable required permissions
4. Restart the app

### Issue: "Permission needed to send images"
**Symptom**: Chat image upload blocked

**Solution**:
1. Tap "Allow" when permission dialog appears
2. If denied, go to Settings > Apps > MediWings > Permissions
3. Enable Photos/Media permission
4. Return to app and try again

---

## 🎨 UI/UX Issues

### Issue: App looks broken or misaligned
**Symptom**: Text overlaps, images missing, layout issues

**Solutions**:
1. Ensure device is running Android 7.0 (API 24) or higher
2. Try rotating device (portrait/landscape)
3. Check if specific to certain screen size
4. Update to latest app version
5. Report issue with device model and Android version

### Issue: Images not loading or showing broken
**Symptom**: Profile pics or banners don't display

**Solutions**:
1. Check internet connection
2. Ensure URLs in database are valid (https://)
3. Clear app cache: Settings > Apps > MediWings > Clear Cache
4. Verify Firebase Storage has the images
5. Check Storage URLs haven't expired

---

## 🔄 Data Sync Issues

### Issue: Changes not reflecting immediately
**Symptom**: Updates take time to appear

**This is Normal**: Firebase real-time sync may take 1-3 seconds

**If Longer Than 5 Seconds**:
1. Check internet connection speed
2. Force refresh by navigating away and back
3. Close and reopen the app
4. Check Firebase is responding normally

### Issue: Old data persists after update
**Symptom**: Editing profile but old data still shows

**Solutions**:
1. Wait 3-5 seconds for sync
2. Navigate away and back to refresh
3. Check Firebase Console to verify data saved
4. Clear app cache if issue persists

---

## 🚨 Crash & Error Handling

### Issue: App crashes without error message
**Symptom**: App closes unexpectedly

**Steps**:
1. Note what you were doing when crash occurred
2. Try to reproduce the crash
3. Check device has sufficient storage and memory
4. Update to latest app version
5. Clear app data as last resort

### Issue: "Error loading [data]"
**Symptom**: Generic error messages

**Solutions**:
1. Check internet connection
2. Verify Firebase services are running
3. Try logging out and back in
4. Clear app cache
5. Report persistent errors with details

---

## 🔧 Advanced Troubleshooting

### Full App Reset
If multiple issues persist:

1. **Backup Important Data** (if possible)
2. **Clear App Data**:
   - Settings > Apps > MediWings
   - Storage > Clear Data
   - Clear Cache
3. **Reinstall App** (if available)
4. **Log in Fresh**
5. **Test Functionality**

### Debug Mode (Developers)
To enable detailed logging:

1. Build debug version: `./gradlew assembleDebug`
2. Install on device: `./gradlew installDebug`
3. Monitor logs: `adb logcat | grep MediWings`
4. Look for error messages or exceptions

### Firebase Console Checks
Verify Firebase setup:

1. **Authentication**:
   - Users tab shows registered users
   - Email/Password provider is enabled

2. **Realtime Database**:
   - Data structure looks correct
   - Rules allow authenticated access
   - No quota limits reached

3. **Storage**:
   - Files are being uploaded
   - Storage rules configured
   - No quota limits reached

4. **Cloud Messaging** (for notifications):
   - Service is enabled
   - Server key configured (if using)

---

## 📞 Getting Help

### Information to Provide When Reporting Issues

1. **Device Info**:
   - Device model (e.g., Samsung Galaxy S21)
   - Android version (e.g., Android 13)
   - App version

2. **Issue Details**:
   - What were you trying to do?
   - What happened instead?
   - Error message (exact text if any)
   - Steps to reproduce

3. **Environment**:
   - User role (Student/Worker/Admin)
   - Internet connection type (WiFi/Mobile)
   - When issue started occurring

4. **Attempted Solutions**:
   - What have you tried already?
   - Did anything work temporarily?

### Contact Channels
- Check app documentation first
- Review this troubleshooting guide
- Contact development team with detailed issue report
- Include screenshots if possible

---

## ✅ Prevention Tips

### Best Practices

1. **Keep App Updated**: Install updates when available
2. **Stable Internet**: Use WiFi for uploads when possible
3. **Optimize Files**: Compress images before uploading
4. **Regular Logout**: Logout and login periodically to refresh state
5. **Clear Cache**: Monthly cache clearing prevents issues
6. **Backup Data**: Keep copies of important documents offline
7. **Monitor Storage**: Ensure device has adequate free space
8. **Check Permissions**: Verify required permissions are granted

### Common Mistakes to Avoid

- ❌ Don't upload files over 1MB
- ❌ Don't use app without internet connection
- ❌ Don't enter wrong email format
- ❌ Don't use weak passwords (< 6 characters)
- ❌ Don't clear app data unless necessary
- ❌ Don't ignore permission requests
- ❌ Don't try to chat without logging in
- ❌ Don't upload corrupted or invalid files

---

## 🔍 Error Code Reference

### Common Firebase Error Codes

| Error Code | Meaning | Solution |
|------------|---------|----------|
| `auth/invalid-email` | Email format is wrong | Check email format |
| `auth/user-not-found` | No account with that email | Register first |
| `auth/wrong-password` | Incorrect password | Check password |
| `auth/email-already-in-use` | Email already registered | Login instead |
| `auth/network-request-failed` | No internet connection | Check connection |
| `storage/unauthorized` | No permission to access | Check Firebase rules |
| `storage/quota-exceeded` | Storage quota full | Contact admin |
| `database/permission-denied` | No access to data | Check Firebase rules |

---

## 📚 Related Documentation

- [README.md](README.md) - Main documentation
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Setup instructions
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Testing procedures
- [FIREBASE_INTEGRATION_CHECKLIST.md](FIREBASE_INTEGRATION_CHECKLIST.md) - Firebase setup

---

**Last Updated**: February 5, 2026
**Version**: 1.0
**Status**: Production-Ready

---

💡 **Tip**: Bookmark this guide for quick reference when issues arise!
