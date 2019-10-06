#
# Builds using GCC 7.x on Bionic
#

FROM ubuntu:bionic
MAINTAINER H. Birch Tree

ENV CMAKE_VER="3.15"
ENV CMAKE_RELVER="3.15.0"

RUN apt-get -qy update && apt-get -qy install \
        git nano wget \
        ninja-build \
        mingw-w64 make

# Get a newer CMake version than Xenial has
RUN wget https://cmake.org/files/v${CMAKE_VER}/cmake-${CMAKE_RELVER}-Linux-x86_64.sh\
	 -O cmake.sh && \
	chmod +x cmake.sh && \
	./cmake.sh --prefix=/usr/local --skip-license --exclude-subdir

RUN groupadd coffeeusers -g 1000
RUN useradd coffee -m -u 1000

# Set up base directories for building
USER coffee
WORKDIR /home/coffee
RUN mkdir -p project bridge build/bin build/libs build/out
USER root

WORKDIR /home/coffee
USER coffee

VOLUME ["/home/coffee/project","/home/coffee/build/libs","/home/coffee/build/out"]
