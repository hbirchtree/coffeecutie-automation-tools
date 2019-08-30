#
# This Dockerfile will create an environment for building against the Steam Runtime as a means of staying portable on Linux
# Any dependencies needed to build will be installed
# The build process will still be performed manually
#

FROM ubuntu:trusty
MAINTAINER H. Birch Tree

ENV USER coffee
ENV USER_ID 1000

ENV CMAKE_VER="3.15"
ENV CMAKE_RELVER="3.15.0"
ENV PROJECT_VOLUME "/home/${USER}/project"
ENV BUILD_VOLUME "/home/${USER}/build"

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
        ninja-build build-essential \
        clang-3.6 clang++-3.6 libc++-dev \
        steamrt-dev \
        libqt5network5 \
        wget git lsb-release \
	    curl

# Get a newer CMake version than Xenial has
RUN wget https://cmake.org/files/v${CMAKE_VER}/cmake-${CMAKE_RELVER}-Linux-x86_64.sh\
	 -O cmake.sh && \
	chmod +x cmake.sh && \
	./cmake.sh --prefix=/usr/local --skip-license --exclude-subdir

#Create user directory + group
RUN groupadd buildusers -g ${USER_ID}
RUN useradd ${USER} -m -u ${USER_ID}

USER ${USER}
RUN mkdir -p    ${PROJECT_VOLUME} \
                ${BUILD_VOLUME}/out

VOLUME ["${PROJECT_VOLUME}", "${BUILD_VOLUME}", "${BUILD_VOLUME}/out"]

WORKDIR ${BUILD_VOLUME}
