#
# This Dockerfile will construct an Android build environment fit to support building CoffeeCutie.
# The Android SDK, NDK and its dependencies will be installed and usable with the attached android-native-build.sh script
#

# Stick with an LTS release, it has the appropriate CMake version
FROM hbirch/ubuntu-cmake:latest
MAINTAINER H. Birch Tree

ARG USERNAME="coffee"
ARG ANDK_VER="15c"
ENV ASDK_VER="24.4.1"

ENV ANDROID_SDK="/home/${USERNAME}/android-sdk-linux"
ENV ANDROID_NDK="/home/${USERNAME}/android-ndk-linux"
ENV ANDROID_HOME="/home/${USERNAME}/android-sdk-linux"

# Install dependencies for Android SDK/NDK as well as build tools
RUN dpkg --add-architecture i386
RUN apt-get -qy update && apt-get -qy install \
    build-essential \
	openjdk-8-jdk-headless \
	git openssl ninja-build make \
	libc6:i386 libstdc++6:i386 wget \
	imagemagick unzip \
	lsb-release xz-utils \
    mercurial

# Create users for dev
RUN useradd ${USERNAME} -m -u 1000

# Set up the Android NDK and SDK
WORKDIR /home/${USERNAME}
USER ${USERNAME}

# Download SDK, NDK and fix the SDK
# We remove the NDK platforms we do not intend to use
RUN \
	wget -q https://dl.google.com/android/repository/android-ndk-r${ANDK_VER}-linux-x86_64.zip \
		-O android-ndk-linux-x86_64.zip && \
	unzip android-ndk-linux-x86_64.zip && \
	mv android-ndk-r${ANDK_VER} android-ndk-linux && \
	rm android-ndk-linux-x86_64.zip && \
    \
    \
	wget -q http://dl.google.com/android/android-sdk_r${ASDK_VER}-linux.tgz \
		-O android-sdk.tgz && \
	tar xf android-sdk.tgz && \
	/bin/bash -c "( sleep 5 && while [ 1 ]; do sleep 1; echo y; done ) |\
		 android-sdk-linux/tools/android update sdk -s -a -u \
            --filter 'tools,build-tools-26.0.2,build-tools-28.0.2,build-tools-28.0.3,build-tools-29.0.3,build-tools,platform-tools,android-19,android-21,android-23,android-24,android-25,android-26,android-27,android-28,android-29'" && \
	rm android-sdk.tgz && \
	rm -r \
	    ${ANDROID_NDK}/platforms/android-12 \
        ${ANDROID_NDK}/platforms/android-13 \
        ${ANDROID_NDK}/platforms/android-14 \
        ${ANDROID_NDK}/platforms/android-15 \
        ${ANDROID_NDK}/platforms/android-16 \
        ${ANDROID_NDK}/platforms/android-17 \
        ${ANDROID_NDK}/platforms/android-18 \
        ${ANDROID_NDK}/platforms/android-22 \
        ${ANDROID_NDK}/platforms/android-26 || true

# Install some script-sauce to create our dev environment
WORKDIR /home/${USERNAME}

RUN ["/bin/bash","-c","echo 'export PATH=$ANDROID_NDK:$ANDROID_SDK/tools:$ANDROID_SDK/platform-tools:$PATH' >> .bashrc"]
RUN ["/bin/bash","-c","echo 'export ANDROID=$(which android)' >> .bashrc"]

# Set the final working directory and user
WORKDIR /home/${USERNAME}
USER coffee

RUN mkdir -p project bridge build

VOLUME ["/home/${USERNAME}/project","/home/${USERNAME}/bridge","/home/${USERNAME}/build"]
