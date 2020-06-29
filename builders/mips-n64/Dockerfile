FROM hbirch/ubuntu-cmake:latest

#
# N64 toolchain build based on DragonMinded/libdragon
#

ARG GCC_VER=9.2.0
ARG BINUTILS_VER=2.33.1
ARG NEWLIB_VER=3.2.0

RUN apt update -qy && \
    apt install -y build-essential git ninja-build wget \
            libgmp3-dev libmpfr-dev libmpc-dev \
            file zlib1g-dev texinfo

RUN mkdir /build

WORKDIR /build
RUN \
    wget ftp://ftp.gnu.org/gnu/gcc/gcc-$GCC_VER/gcc-$GCC_VER.tar.xz -O gcc.tar.xz && \
    wget ftp://ftp.gnu.org/gnu/binutils/binutils-$BINUTILS_VER.tar.xz -O binutils.tar.xz && \
    wget ftp://sourceware.org/pub/newlib/newlib-$NEWLIB_VER.tar.gz -O newlib.tar.gz && \
    \
    tar xvf gcc.tar.xz && \
    tar xvf binutils.tar.xz && \
    tar xvf newlib.tar.gz && \
    \
    mkdir gcc_build binutils_build newlib_build && \
    \
    rm gcc.tar.xz binutils.tar.xz newlib.tar.gz && \
    \
    mv gcc-$GCC_VER gcc && \
    mv binutils-$BINUTILS_VER binutils && \
    mv newlib-$NEWLIB_VER newlib && \
    \
    mkdir -p /$GCC_TARGET/bin

ARG GCC_TARGET=mips
ARG GCC_ARCH=mips32r2
ARG GCC_TUNE=vr4300

RUN PATH=$PATH:/$GCC_TARGET/bin && \
    cd binutils_build && \
    ../binutils/configure --prefix=/$GCC_TARGET --target=$GCC_TARGET --with-cpu=$GCC_ARCH$GCC_TUNE && \
    make -j$(nproc) && \
    make -j$(nproc) install && \
    cd .. && \
    \
    cd gcc_build && \
    ../gcc/configure \
                --prefix=/$GCC_TARGET \
                --target=$GCC_TARGET --with-arch=$GCC_TUNE --with-tune=$GCC_TUNE \
                --enable-languages=c,c++,lto \
                --enable-lto \
                --with-newlib --disable-libssp --enable-multilib \
                --with-headers=/build/newlib/newlib/libc/include \
                --enable-libstdcxx-time=yes \
                --disable-shared --with-gcc --disable-threads --disable-nls --with-system-zlib && \
    make all-gcc all-target-libgcc -j$(nproc) && \
    make install-gcc install-target-libgcc -j$(nproc) && \
    cd .. && \
    \
    cd newlib_build && \
    PATH=$PATH:/$GCC_TARGET/bin CFLAGS="-O2" CXXFLAGS="-O2" ../newlib/configure \
                --prefix=/$GCC_TARGET \
                --target=$GCC_TARGET --with-cpu=$GCC_ARCH$GCC_TUNE \
                --disable-threads --disable-libssp && \
    PATH=$PATH:/$GCC_TARGET/bin make -j$(nproc) && \
    PATH=$PATH:/$GCC_TARGET/bin make -j$(nproc) install && \
    cd .. && \
    \
    cd gcc_build && \
    make all && \
    make install && \
    cd .. && \
    rm -r /build

RUN bash -c 'ln -s /$GCC_TARGET/bin/* /usr/bin/'

ENV N64CHAIN_DIR /mips64-elf

WORKDIR /

