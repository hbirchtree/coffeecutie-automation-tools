#
# This Dockerfile will create an environment for building against the Steam Runtime as a means of staying portable on Linux
# Any dependencies needed to build will be installed
# The build process will still be performed manually
#

FROM ubuntu:trusty
MAINTAINER H. Birch Tree

ENV USER coffee
ENV USER_ID 1000

ENV PROJECT_VOLUME "/home/${USER}/project"
ENV BUILD_VOLUME "/home/${USER}/build"

ENV PRELOAD_SPEC x86_64-linux-steam

ENV TOOLCHAIN_FILE ${PROJECT_VOLUME}/cmake/Toolchains/native-linux-generic.toolchain.cmake
ENV PRELOAD_FILE ${PROJECT_VOLUME}/cmake/Preload/${PRELOAD_SPEC}.cmake

# We need newer CMake, 3.1+
RUN apt-get -qy update && apt-get -qy install software-properties-common
RUN add-apt-repository ppa:george-edison55/cmake-3.x

# Steam beta repository
#RUN echo "deb http://repo.steampowered.com/steamrt/ scout_beta main" >> /etc/apt/sources.list
# Steam non-beta repository
RUN echo "deb http://repo.steampowered.com/steamrt/ scout main" >> /etc/apt/sources.list

ADD key.gpg /key.gpg

RUN cat /key.gpg | apt-key add -
RUN rm /key.gpg

RUN apt-get -qy update && apt-get -qy install \
        cmake ninja-build build-essential gdb \
        steamrt-dev \
        libqt5network5

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

USER ${USER}
RUN mkdir -p    ${PROJECT_VOLUME} \
                ${BUILD_VOLUME}/out

VOLUME ["${PROJECT_VOLUME}", "${BUILD_VOLUME}", "${BUILD_VOLUME}/out"]

WORKDIR ${BUILD_VOLUME}
CMD ["sh","-c","/usr/bin/cmake -C${PRELOAD_FILE} -DCMAKE_TOOLCHAIN_FILE=${TOOLCHAIN_FILE} ${PROJECT_VOLUME} -GNinja && cmake --build . --target install"]
