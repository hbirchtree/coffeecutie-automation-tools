#!/bin/bash

set -e

BINUTILS_VER=2.42
GCC_VER=13.2.0
GLIBC_VER=2.5.1

cd $(dirname $0)

BASE_DIR=$PWD
SRC_DIR=$PWD/src

ARCH=${ARCH:-arm-linux-gnueabi}

INSTALL_DIR=$PWD/$ARCH

mkdir -p src
pushd src

if [ ! -d binutils ]; then
    wget https://ftp.gnu.org/gnu/binutils/binutils-$BINUTILS_VER.tar.xz -O binutils.tar.xz
    tar xf binutils.tar.xz
    mv binutils-$BINUTILS_VER binutils
fi

if [ ! -d glibc ]; then
    wget https://ftp.gnu.org/gnu/glibc/glibc-$GLIBC_VER.tar.bz2 -O glibc.tar.bz2
    tar xf glibc.tar.bz2
    mv glibc-$GLIBC_VER glibc
    cp ../glibc-configure.compat glibc/configure
fi

if [ ! -d gcc ]; then
    wget https://ftp.gnu.org/gnu/gcc/gcc-$GCC_VER/gcc-$GCC_VER.tar.xz -O gcc.tar.xz
    tar xf gcc.tar.xz
    mv gcc-$GCC_VER gcc
    pushd gcc
    contrib/download_prerequisites
    popd
fi

rm -f binutils.tar.xz gcc.tar.xz
popd # src dir

mkdir -p build/$ARCH/{binutils,glibc,gcc} $INSTALL_DIR
pushd build/$ARCH

#
# BINUTILS BUILD
#
pushd binutils
$SRC_DIR/binutils/configure \
    --prefix=$INSTALL_DIR \
    --target=$ARCH \
    --with-sysroot=$INSTALL_DIR/$ARCH \
    --disable-nls \
    --enable-lto \
    --enable-gdb \
    --enable-gold
make -j$(nproc) && make install
popd # binutils dir

export PATH=$INSTALL_DIR/bin:$PATH

#
# GCC BUILD, PHASE 1
#
pushd gcc
DWARF_FLAG=""
$SRC_DIR/gcc/configure \
    --prefix=$INSTALL_DIR \
    --target=$ARCH \
    --disable-multilib \
    --enable-gold \
    --enable-lto \
    --enable-languages=c,c++ \
    --without-headers \
    --with-newlib
make -j$(nproc) all-gcc all-target-libgcc && make install-gcc install-target-libgcc
popd # gcc dir

#
# glibc
#
pushd glibc
CC=$INSTALL_DIR/bin/$ARCH-gcc $SRC_DIR/glibc/configure \
    --prefix=$INSTALL_DIR \
    --target=$ARCH \
    --with-sysroot=$INSTALL_DIR/$ARCH \
    libc_cv_forced_unwind=yes
make -j$(nproc) && make install
popd # glibc dir

#
# GCC BUILD, PHASE 2
#
#pushd gcc
#make -j$(nproc) && make install
#popd # gcc dir

popd # build dir

pushd $ARCH
tar -c -I 'xz -9 -T0' -f ../maemo-$ARCH.tar.xz *
popd

echo "CT_BINUTILS_VERSION=\"$BINUTILS_VER\"
CT_GLIBC_VERSION=\"$BINUTILS_VER\"
CT_MINGW_VERSION=\"$MINGW_VER\"
CT_GCC_VERSION=\"$GCC_VER\"" > $ARCH.manifest
