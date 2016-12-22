#
# This Dockerfile will create a stock compilation environment on Ubuntu LTS
# The listed dependencies might aid in finding dependencies for other distributions
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

ENV USER coffee
ENV USER_ID 1000

ENV PROJECT_VOLUME "/home/${USER}/project"
ENV BUILD_VOLUME "/home/${USER}/build"

ENV PRELOAD_SPEC x86_64-linux-generic

ENV TOOLCHAIN_FILE ${PROJECT_VOLUME}/cmake/Toolchains/native-linux-generic.toolchain.cmake
ENV PRELOAD_FILE ${PROJECT_VOLUME}/cmake/Preload/${PRELOAD_SPEC}.cmake

ENV CC /usr/bin/clang-3.8
ENV CXX /usr/bin/clang++-3.8

# For the cases when you need something extra, eg.:
# - qt5-default
# - libpcl-dev
ENV EXTRA_PACKAGES ""

RUN apt-get -qy update && apt-get -qy install software-properties-common

RUN add-apt-repository ppa:alexlarsson/flatpak

RUN apt-get -qy update && apt-get -qy install \
	cmake ninja-build build-essential \
	clang-3.8 \
	doxygen swig3.0 \
        libsdl2-dev libopenal-dev libssl-dev libunwind-dev \
	libavcodec-dev libavdevice-dev \
	libavformat-dev libswresample-dev \
	libswscale-dev \
        libqt5network5 \
        wget \
        flatpak snapcraft ${EXTRA_PACKAGES}

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

WORKDIR /home/${USER}
USER ${USER}
RUN mkdir -p    ${PROJECT_VOLUME} \
                ${BUILD_VOLUME}/out

VOLUME ["${PROJECT_VOLUME}", "${BUILD_VOLUME}", "${BUILD_VOLUME}/out"]

WORKDIR ${BUILD_VOLUME}
CMD ["sh","-c","/usr/bin/cmake -C${PRELOAD_FILE} -DCMAKE_TOOLCHAIN_FILE=${TOOLCHAIN_FILE} ${PROJECT_VOLUME} -GNinja && cmake --build . --target install"]
