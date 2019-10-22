#
# Compiling for Ubuntu LTS, can use arguments to compile for Bionic
#
FROM hbirch/ubuntu-cmake:3.15
MAINTAINER H. Birch Tree

ENV CLANG_VER 8

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

RUN apt-get -qy update && apt-get -qy install software-properties-common && \
    add-apt-repository ppa:alexlarsson/flatpak

RUN apt-get -qy update && apt-get -qy install \
    ninja-build build-essential \
    clang-${CLANG_VER} \
    doxygen swig3.0 \
    libunwind-dev \
    libtiff5-dev \
    wget git lsb-release \
    flatpak snapcraft \
    curl lcov \
    squashfs-tools \
    libxext-dev \
    && \
    apt-get -qy build-dep \
    libsdl2-dev libopenal-dev

# Download NVIDIA GDK, for GPU querying
RUN wget -q \
    http://developer.download.nvidia.com/compute/cuda/${GDK_URL} \
    -O nvidia-gdk.run && \
    chmod +x nvidia-gdk.run && \
    ./nvidia-gdk.run --silent && \
    rm nvidia-gdk.run

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

WORKDIR /home/${USER}
USER ${USER}
RUN mkdir -p ${PROJECT_VOLUME} ${BUILD_VOLUME}/out

WORKDIR ${BUILD_VOLUME}
