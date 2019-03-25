FROM fedora:28
MAINTAINER H. Birch Tree

RUN dnf -qy update || true && dnf -qy install \
    cmake ninja-build gcc-c++ \
    openal-soft-devel SDL2-devel\
    openssl-devel libunwind-devel \
    libXrender-devel \
    flatpak swig \
    git \
    python \
    make

RUN useradd coffee
USER coffee
