FROM fedora:31
MAINTAINER H. Birch Tree

ARG CMAKE_VER="3.16"
ARG CMAKE_RELVER="3.16.2"

RUN dnf -qy update || true && dnf -y install \
    ninja-build gcc-c++ \
    libunwind-devel \
    libXrender-devel \
    tbb-devel \
    flatpak swig \
    git wget \
    python \
    make \
    patch \
    dnf-plugins-core \
    && \
    dnf -y builddep \
    openssl-devel SDL2-devel openal-soft-devel

RUN wget https://cmake.org/files/v${CMAKE_VER}/cmake-${CMAKE_RELVER}-Linux-x86_64.sh\
    -O cmake.sh && \
    chmod +x cmake.sh && \
    ./cmake.sh --prefix=/usr/local --skip-license --exclude-subdir && \
    rm cmake.sh

RUN useradd coffee
USER coffee
