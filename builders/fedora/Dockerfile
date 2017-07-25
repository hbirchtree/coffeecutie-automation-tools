FROM fedora:25
MAINTAINER H. Birch Tree

RUN dnf -qy update || true && dnf -qy install \
    cmake ninja-build gcc-c++ \
    openal-soft-devel SDL2-devel\
    openssl-devel libunwind-devel \
    assimp-devel \
    libXrender-devel \
    flatpak swig

RUN useradd coffee
USER coffee
