# Use an older version for better glibc compatibility
FROM ubuntu:focal
 
ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_NONINTERACTIVE_SEEN true
RUN { echo 'tzdata tzdata/Areas select Etc'; echo 'tzdata tzdata/Zones/Etc select UTC'; } | debconf-set-selections

RUN apt update && apt install -y \
    build-essential gcc g++ gperf bison flex texinfo help2man make libncurses5-dev \
    python3-dev autoconf automake libtool libtool-bin gawk wget bzip2 xz-utils unzip \
    patch libstdc++6 rsync git meson ninja-build jq \
    \
    bc cpio && \
    apt clean && \
    rm -r /var/cache/apt

ENV CTNG_VERSION=1.26.0
ENV CTNG_SOURCE=https://github.com/crosstool-ng/crosstool-ng/archive/refs/tags/crosstool-ng-$CTNG_VERSION.tar.gz

RUN \
    mkdir /ctng && cd /ctng && wget $CTNG_SOURCE -O src.tar.gz && \
    tar xvf src.tar.gz && mv crosstool-ng-crosstool-ng-$CTNG_VERSION/ src/ && \
    cd src/ && ./bootstrap && ./configure --prefix=/usr && make && make install

ARG CTNG_UID=1000
ARG CTNG_GID=1000
RUN groupadd -g $CTNG_GID ctng
RUN useradd -d /home/ctng -m -g $CTNG_GID -u $CTNG_UID -s /bin/bash ctng

