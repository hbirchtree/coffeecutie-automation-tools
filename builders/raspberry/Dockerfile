#
# This Dockerfile is designed for cross-compiling to Raspberry Pi targets
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN dpkg --add-architecture i386

RUN apt-get -qy update && apt-get -qy install \
        cmake ninja-build \
        git mercurial \
        curl bc automake \
        flex \
        libstdc++6:i386 \
        libz1:i386 \
        ncurses-dev \
        runit

WORKDIR /cross-rpi
RUN git clone https://github.com/raspberrypi/tools.git /cross-rpi
RUN mkdir -p /usr/local/bin
RUN ln -s /cross-rpi/arm-bcm2708/arm-rpi-4.9.3-linux-gnueabihf/bin/* /usr/local/bin/

#Create user directory + group
RUN groupadd coffeeusers -g 1000
RUN useradd coffee -m -u 1000

WORKDIR /home/coffee
USER coffee

RUN mkdir -p project bridge build/bin build/lib

VOLUME ["/home/coffee/project","/home/coffee/bridge","/home/coffee/build/bin","/home/coffee/build/lib"]
