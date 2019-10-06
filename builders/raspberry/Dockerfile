#
# This Dockerfile is designed for cross-compiling to Raspberry Pi targets
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

ARG RASPBERRY_TOOLCHAIN=arm-rpi-4.9.3-linux-gnueabihf
ENV RASPBERRY_CROSSPATH /cross-rpi
ENV USER coffee
ENV USER_ID 1000

ENV CMAKE_VER="3.15"
ENV CMAKE_RELVER="3.15.0"
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
        curl make

# Get a newer CMake version than Xenial has
RUN wget https://cmake.org/files/v${CMAKE_VER}/cmake-${CMAKE_RELVER}-Linux-x86_64.sh\
	 -O cmake.sh && \
	chmod +x cmake.sh && \
	./cmake.sh --prefix=/usr/local --skip-license --exclude-subdir

# Set up toolchain files
# We only fetch the relevant toolchain
WORKDIR ${RASPBERRY_CROSSPATH}
COPY gcc-arm-gnueabihf.tar.xz compiler.tar.xz
RUN tar xvf compiler.tar.xz
RUN mkdir -p /usr/local/bin
RUN bash -c 'ln -s ${RASPBERRY_CROSSPATH}/gcc-arm*/bin/* /usr/local/bin/'

# Old GCC 4.9
#RUN git init
#RUN git remote add -f origin https://github.com/raspberrypi/tools.git
#RUN git config core.sparseCheckout true
#RUN echo "arm-bcm2708/${RASPBERRY_TOOLCHAIN}/" >> .git/info/sparse-checkout
#RUN git pull origin master

#RUN mkdir -p /usr/local/bin

#RUN ln -s ${RASPBERRY_CROSSPATH}/arm-bcm2708/${RASPBERRY_TOOLCHAIN}/bin/* \
#          /usr/local/bin/

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
