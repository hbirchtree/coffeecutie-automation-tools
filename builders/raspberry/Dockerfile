#
# This Dockerfile is designed for cross-compiling to Raspberry Pi targets
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

ARG RASPBERRY_TOOLCHAIN=arm-rpi-4.9.3-linux-gnueabihf
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
        cmake ninja-build \
        git swig3.0 \
        libstdc++6:i386 \
        libz1:i386 \
        libqt5network5 \
        wget lsb-release

# Set up toolchain files
# We only fetch the relevant toolchain
WORKDIR ${RASPBERRY_CROSSPATH}
RUN git init
RUN git remote add -f origin https://github.com/raspberrypi/tools.git
RUN git config core.sparseCheckout true
RUN echo "arm-bcm2708/${RASPBERRY_TOOLCHAIN}/" >> .git/info/sparse-checkout
RUN git pull origin master

RUN mkdir -p /usr/local/bin

RUN ln -s ${RASPBERRY_CROSSPATH}/arm-bcm2708/${RASPBERRY_TOOLCHAIN}/bin/* \
          /usr/local/bin/

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

WORKDIR /home/${USER}
USER ${USER}
RUN mkdir -p    ${PROJECT_VOLUME} \
                bridge \
                ${BUILD_VOLUME}/out

VOLUME ["${PROJECT_VOLUME}", "${BUILD_VOLUME}", "${BUILD_VOLUME}/out", "/home/${USER}/bridge", "${RASPBERRY_SDK_LOCATION}"]

WORKDIR ${BUILD_VOLUME}
CMD ["sh","-c","/usr/bin/cmake -C${PRELOAD_FILE} -DCMAKE_TOOLCHAIN_FILE=${TOOLCHAIN_FILE} -DRASPBERRY_SDK=${RASPBERRY_SDK_LOCATION} ${PROJECT_VOLUME} -GNinja && cmake --build . --target install"]
