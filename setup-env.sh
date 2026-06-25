#!/bin/bash
# Android SDK
export ANDROID_HOME=/home/openclaw/android-sdk
export ANDROID_SDK_ROOT=/home/openclaw/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/36.0.0

# Java 21
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk@21/21.0.11/libexec
export PATH=$PATH:$JAVA_HOME/bin

echo "✅ Android SDK: $ANDROID_HOME"
echo "✅ JAVA_HOME: $JAVA_HOME"
echo "✅ sdkmanager: $(which sdkmanager 2>/dev/null || echo 'not found')"
echo "✅ java: $(which java 2>/dev/null || echo 'not found')"
