#!/bin/bash

# MediWings Firebase Configuration Check Script
# This script helps verify Firebase setup before building

echo "=================================="
echo "MediWings Firebase Setup Checker"
echo "=================================="
echo ""

# Check if google-services.json exists
if [ -f "app/google-services.json" ]; then
    echo "✓ google-services.json found"
    
    # Check package name in the file
    package_name=$(grep -o '"package_name": "[^"]*"' app/google-services.json | head -1 | cut -d'"' -f4)
    
    if [ "$package_name" == "com.tripplanner.mediwings" ]; then
        echo "✓ Package name is correct: $package_name"
    else
        echo "✗ ERROR: Package name mismatch!"
        echo "  Expected: com.tripplanner.mediwings"
        echo "  Found: $package_name"
        echo "  Please download the correct google-services.json from Firebase Console"
        exit 1
    fi
else
    echo "✗ ERROR: google-services.json NOT found!"
    echo ""
    echo "To fix this:"
    echo "1. Go to https://console.firebase.google.com"
    echo "2. Select your MediWings project"
    echo "3. Go to Project Settings (gear icon)"
    echo "4. Under 'Your apps', find the Android app"
    echo "5. Download google-services.json"
    echo "6. Place it in: app/google-services.json"
    echo ""
    exit 1
fi

# Check if build.gradle.kts exists
if [ -f "app/build.gradle.kts" ]; then
    echo "✓ build.gradle.kts found"
else
    echo "✗ ERROR: build.gradle.kts not found!"
    exit 1
fi

# Check if Firebase dependencies are present
if grep -q "com.google.gms:google-services" build.gradle.kts || grep -q "com.google.gms:google-services" app/build.gradle.kts; then
    echo "✓ Google Services plugin configured"
else
    echo "⚠ Warning: Google Services plugin might not be configured"
fi

echo ""
echo "=================================="
echo "Firebase Setup Check Complete!"
echo "=================================="
echo ""
echo "Next Steps:"
echo "1. Ensure internet connection is available"
echo "2. Run: ./gradlew assembleDebug"
echo "3. If build fails, check:"
echo "   - Internet connectivity"
echo "   - Firebase Console configuration"
echo "   - Gradle sync in Android Studio"
echo ""
echo "For more help, see:"
echo "- README.md"
echo "- UPGRADE_DOCUMENTATION.md"
echo "- TESTING_VERIFICATION_GUIDE.md"
echo ""
