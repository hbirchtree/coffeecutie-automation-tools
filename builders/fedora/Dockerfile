FROM fedora:31
MAINTAINER H. Birch Tree

RUN dnf -qy update || true && dnf -y install \
    cmake ninja-build gcc-c++ \
    libunwind-devel \
    libXrender-devel \
    tbb-devel \
    flatpak swig \
    git \
    python \
    make \
    patch \
    dnf-plugins-core \
    && \
    dnf -y builddep \
    openssl-devel SDL2-devel openal-soft-devel

RUN useradd coffee
USER coffee
