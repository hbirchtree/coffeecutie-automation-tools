#!/bin/bash

set -e

BINUTILS_VER=2.42
GCC_VER=13.2.0
MINGW_VER=6.0.1

cd $(dirname $0)

BASE_DIR=$PWD
SRC_DIR=$PWD/src

ARCH=${ARCH:-x86_64}
VCRT=ucrt

INSTALL_DIR=$PWD/mingw-w64-$ARCH

mkdir -p src
pushd src

if [ ! -d binutils ]; then
    wget https://ftp.gnu.org/gnu/binutils/binutils-$BINUTILS_VER.tar.xz -O binutils.tar.xz
    tar xf binutils.tar.xz
    mv binutils-$BINUTILS_VER binutils
fi

if [ ! -d gcc ]; then
    wget https://ftp.gnu.org/gnu/gcc/gcc-$GCC_VER/gcc-$GCC_VER.tar.xz -O gcc.tar.xz
    tar xf gcc.tar.xz
    mv gcc-$GCC_VER gcc
    pushd gcc
    contrib/download_prerequisites
    popd
fi

if [ ! -d mingw ]; then
    wget https://netix.dl.sourceforge.net/project/mingw-w64/mingw-w64/mingw-w64-release/mingw-w64-v$MINGW_VER.zip -O mingw.zip
    unzip mingw.zip
    mv mingw-w64-v$MINGW_VER mingw
fi

rm -f binutils.tar.xz gcc.tar.xz mingw.zip
popd # src dir

mkdir -p build/$ARCH/{binutils,gcc,mingw-headers,mingw-crt,mingw,winpthread} $INSTALL_DIR
pushd build/$ARCH

#
# BINUTILS BUILD
#
pushd binutils
$SRC_DIR/binutils/configure \
    --prefix=$INSTALL_DIR \
    --target=$ARCH-w64-mingw32 \
    --with-sysroot=$INSTALL_DIR/$ARCH-w64-mingw \
    --disable-multilib \
    --disable-nls \
    --enable-lto \
    --enable-gdb
make -j$(nproc) && make install
popd # binutils dir

export PATH=$INSTALL_DIR/bin:$PATH

#
# MINGW-W64 HEADERS
#
pushd mingw-headers
$SRC_DIR/mingw/mingw-w64-headers/configure \
    --prefix=$INSTALL_DIR/$ARCH-w64-mingw32 \
    --host=$ARCH-w64-mingw32 \
    --with-default-msvcrt=$VCRT \
    --enable-idl \
    --enable-secure-api
make install
popd # mingw-headers

#
# GCC BUILD, PHASE 1
#
pushd gcc
DWARF_FLAG=""
if [ $ARCH = "i686" ]; then
    DWARF_FLAG="--disable-sjlj-exceptions --with-dwarf2"
fi
$SRC_DIR/gcc/configure \
    --prefix=$INSTALL_DIR \
    --target=$ARCH-w64-mingw32 \
    --disable-multilib \
    --enable-languages=c,c++ \
    --enable-threads=posix \
    $DWARF_FLAG
make -j$(nproc) all-gcc && make install-gcc
popd # gcc dir

#
# MINGW-W64 CRT BUILD
#
pushd mingw-crt
if [ $ARCH = "x86_64" ]; then
    LIB3264_FLAGS="--enable-lib64 --disable-lib32"
else
    LIB3264_FLAGS="--disable-lib64 --enable-lib32"
fi
$SRC_DIR/mingw/mingw-w64-crt/configure \
    --prefix=$INSTALL_DIR/$ARCH-w64-mingw32 \
    --host=$ARCH-w64-mingw32 \
    --with-default-msvcrt=$VCRT \
    --with-sysroot=$INSTALL_DIR/$ARCH-w64-mingw32 \
    $LIB3264_FLAGS
make && make install
popd # mingw-crt dir

#
# MINGW-W64 BUILD
#
pushd mingw
$SRC_DIR/mingw/configure \
    --prefix=$INSTALL_DIR/$ARCH-w64-mingw32 \
    --host=$ARCH-w64-mingw32 \
    --with-libraries=winpthreads \
    --with-sysroot=$INSTALL_DIR/$ARCH-w64-mingw32 \
    $LIB3264_FLAGS
make && make install
popd # mingw dir

#
# GCC BUILD, PHASE 2
#
pushd gcc
make -j$(nproc) && make install
popd # gcc dir

#
# WINPTHREAD BUILD
#
pushd winpthread
$SRC_DIR/mingw/mingw-w64-libraries/winpthreads/configure \
    --prefix=$INSTALL_DIR/$ARCH-w64-mingw32 \
    --host=$ARCH-w64-mingw32 \
    --disable-shared \
    --enable-static \
    --with-sysroot=$INSTALL_DIR/$ARCH-w64-mingw32
make && make install
popd # winpthread dir

popd # build dir

pushd mingw-w64-$ARCH
tar -c -I 'xz -9 -T0' -f ../windows-$ARCH-w64-mingw32_posix.tar.xz *
popd

echo "CT_BINUTILS_VERSION=\"$BINUTILS_VER\"
CT_GLIBC_VERSION=\"$BINUTILS_VER\"
CT_MINGW_VERSION=\"$MINGW_VER\"
CT_GCC_VERSION=\"$GCC_VER\"" > $ARCH-w64-mingw32.manifest
