#
# This Dockerfile sets up an environment for building with emscripten
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

ENV USNAME="coffee"
ENV EMSDK_DIR="/home/coffee/emsdk_portable"

####
# Installing some dependencies
####
RUN apt-get -qy update && apt-get -qy install \
        wget build-essential git \
        cmake ninja-build \
        python2.7 nodejs \
        default-jre \
	curl

####
# Get the emsdk
####
RUN ln -s /usr/bin/python2.7 /usr/bin/python

RUN adduser -u 1000 $USNAME
USER $USNAME

WORKDIR /home/$USNAME
RUN wget https://s3.amazonaws.com/mozilla-games/emscripten/releases/emsdk-portable.tar.gz \
    && tar -zxvf emsdk-portable.tar.gz \
    && rm emsdk-portable.tar.gz \
    && [ -d emsdk-portable ] && mv emsdk-portable $EMSDK_DIR

####
# Installing latest Emscripten version
####

RUN $EMSDK_DIR/emsdk update
RUN $EMSDK_DIR/emsdk install latest

ENV PATH=$PATH:$EMSDK_DIR:$EMSDK_DIR/clang/fastcomp/build_master_64/bin:$EMSDK_DIR/node/4.1.1_64bit/bin:$EMSDK_DIR/emscripten/master
ENV EMSCRIPTEN_ROOT=$EMSDK_DIR/emscripten/master

RUN $EMSDK_DIR/emsdk activate latest

WORKDIR /

VOLUME ["/code","/build/output"]
