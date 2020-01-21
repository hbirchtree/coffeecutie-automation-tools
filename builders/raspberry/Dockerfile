#
# This Dockerfile is designed for cross-compiling to Raspberry Pi targets
#

FROM hbirch/ubuntu-cmake:latest
MAINTAINER H. Birch Tree

ENV RASPBERRY_CROSSPATH /cross-rpi
ENV USER coffee
ENV USER_ID 1000

ENV RASPBERRY_SDK_LOCATION "/raspi-sdk"
ENV PROJECT_VOLUME "/home/${USER}/project"
ENV BUILD_VOLUME "/home/${USER}/build"

ARG TOOLCHAIN_NAME=gnueabihf-arm-raspberry

ENV TOOLCHAIN_FILE ${PROJECT_VOLUME}/cmake/Toolchains/${TOOLCHAIN_NAME}.toolchain.cmake
ENV PRELOAD_FILE ${PROJECT_VOLUME}/cmake/Preload/raspberry.cmake

RUN dpkg --add-architecture i386

RUN apt-get -qy update && apt-get -qy install \
        ninja-build \
        git swig3.0 \
        libstdc++6:i386 \
        libz1:i386 \
        libqt5network5 \
        wget lsb-release xz-utils \
        curl make build-essential

# Set up toolchain files
# We only fetch the relevant toolchain
WORKDIR ${RASPBERRY_CROSSPATH}
COPY gcc-arm-gnueabihf.tar.xz compiler.tar.xz
RUN tar xvf compiler.tar.xz
RUN mkdir -p /usr/local/bin
RUN bash -c 'ln -s ${RASPBERRY_CROSSPATH}/gcc-arm*/bin/* /usr/local/bin/'

# Download GLES/EGL/bcm_host files
WORKDIR /pi-sysroot
RUN wget https://github.com/raspberrypi/firmware/archive/master.tar.gz \
    && tar xvf master.tar.gz \
    && cp -r firmware-master/hardfp/opt/vc . \
    && rm -r master.tar.gz firmware-master

RUN ln -s /pi-sysroot/vc /opt/vc

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

WORKDIR /home/${USER}
USER ${USER}
RUN mkdir -p ${PROJECT_VOLUME} bridge ${BUILD_VOLUME}/out
