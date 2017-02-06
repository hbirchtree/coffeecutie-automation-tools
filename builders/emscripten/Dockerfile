#
# This Dockerfile sets up an environment for building with emscripten
# NOTE: Untested
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN apt-get -qy update && apt-get -qy install \
        wget build-essential git \
        cmake ninja-build \
        python2.7 nodejs \
        default-jre \
	curl

RUN ln -s /usr/bin/python2.7 /usr/bin/python

WORKDIR /
RUN wget https://s3.amazonaws.com/mozilla-games/emscripten/releases/emsdk-portable.tar.gz \
    && tar -zxvf emsdk-portable.tar.gz \
    && rm emsdk-portable.tar.gz

RUN /emsdk_portable/emsdk update
RUN /emsdk_portable/emsdk install latest

ENV PATH=$PATH:/emsdk_portable:/emsdk_portable/clang/fastcomp/build_master_64/bin:/emsdk_portable/node/4.1.1_64bit/bin:/emsdk_portable/emscripten/master

RUN adduser -u 1000 coffee
USER coffee
RUN /emsdk_portable/emsdk activate latest

VOLUME ["/code","/build/output"]
