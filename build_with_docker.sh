#!/usr/bin/env bash
set -e

echo "🚀 Building Android Water Heater APK using Docker (No local Java/Gradle required)..."

# Build Docker builder image
docker build -t water-heater-android-builder .

# Run container mounting workspace to extract app-debug.apk
docker run --rm -v "$(pwd):/workspace" water-heater-android-builder

echo ""
echo "🎉 SUCCESS! Your Android APK is compiled and saved at:"
echo "👉 $(pwd)/app-debug.apk"
echo ""
echo "📱 To install on your phone:"
echo "1. Send 'app-debug.apk' to yourself on WhatsApp / Email / Drive."
echo "2. Open the file on your Android phone and tap Install!"
