FROM fedora:latest
MAINTAINER H. Birch Tree

RUN dnf -qy update || dnf -qy install \
    cmake ninja-build gcc-c++ \
    openal-soft-devel SDL2-devel\
    openssl-devel libunwind-dev \
    flatpak swig

RUN useradd coffee
USER coffee