#
# Builds using GCC 7.x on Bionic
#

FROM hbirch/ubuntu-cmake:latest
MAINTAINER H. Birch Tree

RUN apt-get -qy update && apt-get -qy install \
        git nano wget \
        ninja-build \
        mingw-w64 make \
        build-essential

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
