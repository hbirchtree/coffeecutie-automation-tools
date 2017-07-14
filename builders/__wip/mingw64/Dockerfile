#
# An environment for compiling MinGW64 Windows builds
#
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN apt-get -qy update && apt-get -qy install \
        git nano wget \
        mercurial \
        cmake ninja-build \
        mingw-w64

RUN groupadd coffeeusers -g 1000
RUN useradd coffee -m -u 1000

# Set up base directories for building
USER coffee
WORKDIR /home/coffee
RUN mkdir -p project bridge build/bin build/libs build/out
USER root

# Check out source for SDL2
RUN mkdir -p /dependencies/SDL/build
RUN mkdir -p /dependencies/SDL/source
RUN hg clone https://hg.libsdl.org/SDL /dependencies/SDL/source

# Check out source for openal-soft
RUN mkdir -p /dependencies/openal/build
RUN mkdir -p /dependencies/openal/source
RUN git clone https://github.com/kcat/openal-soft.git /dependencies/openal/source

# Set environment variables for SDL2
ENV CC=x86_64-w64-mingw32-gcc
ENV CXX=x86_64-w64-mingw32-g++
ENV AS=x86_64-w64-mingw32-as
ENV LD=x86_64-w64-mingw32-ld

WORKDIR /home/coffee
USER coffee

VOLUME ["/home/coffee/project","/home/coffee/build/libs","/home/coffee/build/out"]
