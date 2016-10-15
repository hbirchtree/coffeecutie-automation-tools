#
# This Dockerfile sets up an environment for building with emscripten
# NOTE: Untested
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN apt-get -qy update && apt-get -qy install \
        wget \
        build-essential \
        git \
        cmake ninja-build \
        python2.7 \
        nodejs \
        default-jre

RUN ln -s /usr/bin/python2.7 /usr/bin/python
RUN mkdir -p /build

WORKDIR /build
RUN wget https://s3.amazonaws.com/mozilla-games/emscripten/releases/emsdk-portable.tar.gz
RUN tar -zxvf emsdk-portable.tar.gz

RUN /build/emsdk_portable/emsdk update
RUN /build/emsdk_portable/emsdk install latest

VOLUME ["/code","/build/output"]
