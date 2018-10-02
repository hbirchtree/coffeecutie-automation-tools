#
# Compiling for Ubuntu LTS, can use arguments to compile for Bionic
#

ARG UBU_RELEASE=xenial
ARG CLANG_VER=3.8
ARG BASE_IMAGE ubuntu

FROM ${BASE_IMAGE}:${UBU_RELEASE}
MAINTAINER H. Birch Tree

ENV USER coffee
ENV USER_ID 1000

ENV PROJECT_VOLUME "/home/${USER}/project"
ENV BUILD_VOLUME "/home/${USER}/build"

# We want to use Clang when possible
ENV CC /usr/bin/clang-${CLANG_VER}
ENV CXX /usr/bin/clang++-${CLANG_VER}

# NVIDIA GDK download
# When compiling 32-bit, use:
#   6_5/rel/installers/cuda_340_29_gdk_linux_32.run
#
ENV GDK_URL 7.5/Prod/gdk/gdk_linux_amd64_352_79_release.run

# For the cases when you need something extra, eg.:
# - qt5-default
# - libpcl-dev
ENV EXTRA_PACKAGES ""

RUN apt-get -qy update && apt-get -qy install software-properties-common

RUN add-apt-repository ppa:alexlarsson/flatpak

RUN apt-get -qy update && apt-get -qy install \
    cmake ninja-build build-essential \
    clang-${CLANG_VER} \
    doxygen swig3.0 \
    libsdl2-dev libopenal-dev libssl-dev libunwind-dev \
    libavcodec-dev libavdevice-dev \
    libavformat-dev libswresample-dev \
    libswscale-dev \
    libtiff5-dev \
    libqt5network5 \
    wget git lsb-release \
    flatpak snapcraft ${EXTRA_PACKAGES} \
    curl lcov \
    squashfs-tools

# Download NVIDIA GDK, for GPU querying
RUN wget -q \
    http://developer.download.nvidia.com/compute/cuda/${GDK_URL} \
    -O nvidia-gdk.run
RUN chmod +x nvidia-gdk.run
RUN ./nvidia-gdk.run --silent
RUN rm nvidia-gdk.run

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

WORKDIR /home/${USER}
USER ${USER}
RUN mkdir -p ${PROJECT_VOLUME} ${BUILD_VOLUME}/out

VOLUME ["${PROJECT_VOLUME}", "${BUILD_VOLUME}", "${BUILD_VOLUME}/out"]

WORKDIR ${BUILD_VOLUME}
