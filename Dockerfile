FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk
ENV GRADLE_USER_HOME=/root/.gradle
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:/opt/gradle/bin

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    git \
    libstdc++6 \
    zlib1g \
    && rm -rf /var/lib/apt/lists/*

# Install Gradle 8.11.1
RUN curl -sS -L https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    mv /opt/gradle-8.11.1 /opt/gradle && \
    rm /tmp/gradle.zip

# Download Android SDK Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    curl -sS https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept SDK licenses and install required platforms and build tools
RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

WORKDIR /workspace

# Build Debug APK using isolated project cache directory to avoid host volume lock conflicts
CMD ["bash", "-c", "gradle assembleDebug --no-daemon --project-cache-dir /tmp/gradle-project-cache && cp app/build/outputs/apk/debug/app-debug.apk /workspace/app-debug.apk && echo '🎉 BUILD SUCCESSFUL! app-debug.apk created in /workspace/app-debug.apk'"]
