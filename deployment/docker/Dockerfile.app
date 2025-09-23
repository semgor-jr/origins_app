# Dockerfile для Android приложения (для сборки)
FROM ubuntu:20.04

# Устанавливаем необходимые пакеты
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Устанавливаем Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

RUN mkdir -p $ANDROID_HOME && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip commandlinetools-linux-9477386_latest.zip -d $ANDROID_HOME && \
    rm commandlinetools-linux-9477386_latest.zip

# Устанавливаем Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip && \
    unzip gradle-8.4-bin.zip -d /opt && \
    rm gradle-8.4-bin.zip

ENV PATH=$PATH:/opt/gradle-8.4/bin

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем исходный код
COPY . .

# Собираем приложение
RUN ./gradlew :app:assembleRelease

# Результат сборки будет в app/build/outputs/apk/release/
