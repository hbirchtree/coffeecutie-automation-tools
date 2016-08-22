#
# This Dockerfile is designed for cross-compiling to linux-armeabihf targets
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN apt-get -qy update && apt-get -qy install \
        g++-multilib-arm-linux-gnueabihf \
	cmake ninja-build \
	git mercurial

#Create user directory + group
RUN groupadd coffeeusers -g 1000
RUN useradd coffee -m -u 1000

WORKDIR /home/coffee
USER coffee

RUN mkdir -p project bridge build/bin build/lib

VOLUME ["/home/coffee/project","/home/coffee/bridge","/home/coffee/build/bin","/home/coffee/build/lib"]
