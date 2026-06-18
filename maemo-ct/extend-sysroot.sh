#!/bin/bash

set -e

PACKAGES=(
    libgles1-sgx-img
    libgles1-sgx-img-dev
    libgles2-sgx-img
    libgles2-sgx-img-dev
    libx11-dev
    libxau6
    libxau-dev
    libxcb1-dev
    libxcb-xlib0-dev
    libxdmcp6
    libxdmcp-dev
    opengles-sgx-img-common
    opengles-sgx-img-common-dev
    x11proto-core-dev
    x11proto-input-dev
)
REPO=${REPO:-repo/}
REPO=/home/.extstorage/havard/Maemo/repository.maemo.org/maemo.muarf.org/apt-mirror/mirror/repository.maemo.org/pool/maemo5.0

for PKG in ${PACKAGES[*]}; do
    echo "-- Locating $PKG"
    FILE=$(find $REPO -name "${PKG}_*.deb"| grep 'all\|armel' | sort -nr | head -1)
    echo "-- Found $FILE"
    sudo dpkg-deb -x "$FILE" "$SYSROOT"
    #dpkg-deb -c "$FILE"
done
