# Profile Images and Banner Photos - Testing Guide

## Overview
This guide provides comprehensive testing procedures to verify that profile images and banner photos are correctly uploaded, stored, and displayed across all user roles in the MediWings application.

## Changes Implemented

### 1. Enhanced Image Loading with Error Handling
- **All Glide image loads** now include:
  - `.placeholder(R.drawable.ic_default_avatar)` - Shows default avatar during loading
  - `.error(R.drawable.ic_default_avatar)` - Shows default avatar if loading fails
  - `.circleCrop()` - Ensures circular formatting for profile images
  
### 2. Activity Lifecycle Protection
- **WorkerHomeActivity** now checks `!isFinishing && !isDestroyed` before UI updates
- Prevents crashes when user navigates away during image upload

### 3. Default Avatar Fallback
- When `photoUrl` is null or empty, default avatar is displayed
- No blank spaces in profile or navigation drawer

### 4. Comprehensive Documentation
- Added detailed documentation headers to all key files:
  - `StudentHomeActivity.kt` - Student profile image handling
  - `WorkerHomeActivity.kt` - Worker profile image handling
  - `UserListActivity.kt` - User list thumbnail display
  - `AdminBannerManagementActivity.kt` - Banner management and display

### 5. Banner Display Enhancements
- Banner carousel images now have error handling
- University/institution images have error handling
- Status timeline images have error handling

## Testing Procedures

### Pre-Testing Setup
1. Ensure Firebase project is configured with:
   - Firebase Storage enabled
   - Firestore Database with `users` collection
   - Realtime Database with `users` and `workers` nodes
2. Have at least one test account for each role:
   - Admin
   - Student
   - Worker

### Test 1: Student Profile Image Upload and Display

#### Steps:
1. **Login as Student**
   - Open app and sign in with student credentials
   
2. **Upload Profile Photo**
   - Navigate to profile section
   - Tap on profile picture placeholder/existing image
   - Select "Upload Profile Picture"
   - Choose an image < 1MB from device
   - Verify progress dialog shows upload percentage
   - Wait for "Profile picture updated!" toast message
   
3. **Verify Immediate Display**
   - ✓ Profile section shows uploaded image (circular format)
   - ✓ Navigation drawer header shows uploaded image (circular format)
   - ✓ No blank spaces or broken image icons
   - ✓ Image is clearly visible and properly cropped
   
4. **Test Persistence - App Restart**
   - Close app completely (swipe away from recent apps)
   - Reopen app and login as same student
   - ✓ Profile image still displayed in both locations
   - ✓ No need to re-upload
   
5. **Test Persistence - Different Device**
   - Login with same student account on different device/emulator
   - ✓ Profile image displays correctly
   - ✓ Confirms image stored in cloud, not local cache only

6. **Test Error Scenarios**
   - **Scenario A: Large File**
     - Try uploading image > 1MB
     - ✓ Should show error: "File too large! Please select a file smaller than 1MB..."
   
   - **Scenario B: No Photo URL**
     - Manually delete `photoUrl` from Firestore (using Firebase Console)
     - Restart app
     - ✓ Should show default avatar (ic_default_avatar)
     - ✓ No blank space or crash
   
   - **Scenario C: Corrupted URL**
     - Manually set `photoUrl` to invalid URL in Firestore
     - Restart app
     - ✓ Should show default avatar (error fallback)
     - ✓ No crash or blank space

### Test 2: Worker Profile Image Upload and Display

#### Steps:
1. **Login as Worker**
   - Open app and sign in with worker credentials
   
2. **Upload Profile Photo**
   - Navigate to profile section or documents area
   - Look for profile photo upload option
   - Select image < 1MB
   - Verify upload progress dialog
   - Wait for success message
   
3. **Verify Display**
   - ✓ Navigation drawer shows uploaded image (circular)
   - ✓ Profile section shows uploaded image (circular)
   - ✓ Images appear in user list when admin/student views workers
   
4. **Test Persistence**
   - Restart app, verify image persists
   - Login from different device, verify image displays
   
5. **Test Activity Lifecycle**
   - Start uploading image
   - Immediately press back button or navigate away
   - ✓ App should not crash
   - ✓ Upload should complete in background
   - ✓ Return to profile to see if image updated

### Test 3: User List Display (Admin/Chat View)

#### Steps:
1. **Login as Admin or any user with chat access**
   
2. **View User List**
   - Navigate to user list or chat list
   - ✓ All users with profile photos show circular thumbnails
   - ✓ Users without photos show default avatar
   - ✓ No blank spaces in list items
   
3. **Test with Multiple Users**
   - Ensure list has users with and without photos
   - ✓ Mixed display works correctly
   - ✓ Scrolling is smooth, images load properly
   
4. **Test Network Issues**
   - Enable airplane mode
   - View user list
   - ✓ Previously loaded images may show from cache
   - ✓ New images show placeholder, not blank
   - Disable airplane mode
   - ✓ Images load after network restored

### Test 4: Banner Management and Display (Admin)

#### Steps:
1. **Login as Admin**
   
2. **Upload Banner**
   - Navigate to Banner Management
   - Tap "Add Banner"
   - Select image < 1MB
   - Verify upload progress
   - ✓ Banner appears in list with thumbnail
   
3. **View Banner Preview**
   - Tap "View" on uploaded banner
   - ✓ Full-size preview shows in dialog
   - ✓ Image is clear and properly displayed
   
4. **Verify Banner Display in Home Screens**
   - Logout and login as Student
   - Check home screen for banner carousel
   - ✓ Uploaded banner appears in auto-scrolling carousel
   - ✓ Banner images have proper aspect ratio
   - ✓ No broken images or blank spaces
   
5. **Test Banner Error Handling**
   - Manually corrupt a banner URL in Realtime DB
   - View admin banner list or student home screen
   - ✓ Corrupted banner shows placeholder (ic_menu_gallery)
   - ✓ Other banners still display correctly
   - ✓ No crash

### Test 5: Cross-Role Image Visibility

#### Steps:
1. **Upload as Student**
   - Login as Student A
   - Upload profile photo
   
2. **View as Worker**
   - Login as Worker B
   - Navigate to chat/user list
   - ✓ Student A's profile photo visible in list
   
3. **View as Admin**
   - Login as Admin C
   - View user management/list
   - ✓ Student A's profile photo visible
   - ✓ Worker B's profile photo visible (if uploaded)

### Test 6: Edge Cases and Stress Tests

#### Test 6A: Rapid Role Switching
1. Login as Student → Upload image → Logout
2. Login as Worker → Upload image → Logout
3. Login as Admin → View all users
4. ✓ All images display correctly
5. ✓ No cross-contamination of images between users

#### Test 6B: Concurrent Uploads
1. Open app on two devices with same account
2. Upload different images simultaneously
3. ✓ Last upload wins (expected behavior)
4. ✓ Both devices eventually show same image after sync

#### Test 6C: Large Number of Images
1. As Admin, upload 10+ banners
2. View banner management screen
3. ✓ All thumbnails load without issues
4. ✓ Scrolling remains smooth
5. View student home screen
6. ✓ Banner carousel handles all banners

#### Test 6D: Image Format Variations
1. Test uploading different formats (if supported):
   - JPG
   - PNG
   - (WebP if supported)
2. ✓ All formats display correctly
3. ✓ File size validation works for all formats

## Expected Results Summary

### ✓ Upload Success Criteria
- [x] Progress dialog shows during upload
- [x] Success toast appears after upload
- [x] Image immediately visible after upload
- [x] No errors or crashes during upload process

### ✓ Display Success Criteria
- [x] Profile images show in circular format
- [x] Default avatar shows when no photo exists
- [x] Error avatar shows when URL is invalid
- [x] No blank spaces in any UI area
- [x] Images persist across app restarts
- [x] Images visible on different devices
- [x] Navigation drawer shows profile image

### ✓ Error Handling Success Criteria
- [x] Large files (>1MB) rejected with clear message
- [x] Invalid URLs show fallback avatar
- [x] Network errors don't crash app
- [x] Activity destruction during upload doesn't crash
- [x] Missing photoUrl doesn't cause blank spaces

### ✓ Cross-Role Success Criteria
- [x] Student images visible to Workers/Admins
- [x] Worker images visible to Students/Admins
- [x] Banners visible to all non-admin users
- [x] Image updates sync across all devices

## Troubleshooting

### Issue: Image doesn't appear after upload
**Check:**
1. Firebase Storage rules allow read/write
2. Firestore security rules allow read/write to users collection
3. Internet connection is active
4. photoUrl field exists in Firestore document
5. Download URL is valid (check Firebase Console)

### Issue: Image shows temporarily then disappears
**Check:**
1. Firebase Storage rules haven't expired
2. Download URL is permanent (not temporary)
3. Token in URL is still valid
4. Firestore listener is active (not disconnected)

### Issue: Different images on different devices
**Check:**
1. Both devices are logged into same account
2. Firestore sync is working (check timestamps)
3. No local caching issues (try clearing app cache)
4. Latest upload completed successfully

### Issue: Default avatar shows instead of uploaded image
**Check:**
1. photoUrl field has valid URL string in Firestore
2. URL is accessible (try opening in browser)
3. Glide is not being blocked by network restrictions
4. Firebase Storage object exists at that URL

## Regression Testing

After any future modifications to image handling code, re-run all tests above to ensure:
- No existing functionality broken
- New features work as expected
- Error handling still robust
- Documentation remains accurate

## Code References

| File | Key Functions | Line References |
|------|---------------|-----------------|
| `StudentHomeActivity.kt` | `uploadImage()`, `loadUserData()`, `loadUserDataFromRealtimeDB()` | ~628, ~304, ~359 |
| `WorkerHomeActivity.kt` | `uploadDocument()`, `loadUserData()`, `loadUserDataFromRealtimeDB()` | ~336, ~142, ~198 |
| `UserListActivity.kt` | `UserAdapter.onBindViewHolder()` | ~242 |
| `AdminBannerManagementActivity.kt` | `uploadNewBanner()`, `BannerAdapter.onBindViewHolder()`, `viewBanner()` | ~142, ~273, ~236 |

## Documentation References

For implementation details and architecture, see documentation headers in:
- `StudentHomeActivity.kt` (lines 3-68)
- `WorkerHomeActivity.kt` (lines 3-48)
- `UserListActivity.kt` (lines 3-26)
- `AdminBannerManagementActivity.kt` (lines 3-36)

---

**Last Updated:** 2026-02-10  
**Version:** 1.0  
**Author:** MediWings Development Team
