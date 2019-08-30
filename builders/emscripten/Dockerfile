#
# This Dockerfile sets up an environment for building with emscripten
#

FROM ubuntu:xenial
MAINTAINER H. Birch Tree

ENV CMAKE_VER="3.15"
ENV CMAKE_RELVER="3.15.0"
ENV USNAME="coffee"
ENV EMSDK_DIR="/home/coffee/emsdk_portable"

# Installing some dependencies
RUN apt-get -qy update && apt-get -qy install \
    wget build-essential git ninja-build \
    python2.7 nodejs \
    default-jre \
    curl

# Get a newer CMake version than Xenial has
RUN wget https://cmake.org/files/v${CMAKE_VER}/cmake-${CMAKE_RELVER}-Linux-x86_64.sh\
	 -O cmake.sh && \
	chmod +x cmake.sh && \
	./cmake.sh --prefix=/usr/local --skip-license --exclude-subdir

# Get the emsdk
RUN ln -s /usr/bin/python2.7 /usr/bin/python

RUN adduser -u 1000 $USNAME
USER $USNAME

WORKDIR /home/$USNAME
RUN wget https://s3.amazonaws.com/mozilla-games/emscripten/releases/emsdk-portable.tar.gz \
    && tar -zxvf emsdk-portable.tar.gz \
    && rm emsdk-portable.tar.gz \
    && [ -d emsdk-portable ] && mv emsdk-portable $EMSDK_DIR

# Installing latest Emscripten versio
RUN $EMSDK_DIR/emsdk update
RUN $EMSDK_DIR/emsdk install latest

# Set up environment variables
ENV PATH=$PATH:$EMSDK_DIR:$EMSDK_DIR/clang/fastcomp/build_master_64/bin:$EMSDK_DIR/node/4.1.1_64bit/bin:$EMSDK_DIR/emscripten/master
ENV EMSCRIPTEN_ROOT=$EMSDK_DIR/emscripten/master

RUN $EMSDK_DIR/emsdk activate latest

RUN ln -s $EMSDK_DIR/emscripten/* $EMSDK_DIR/emscripten/master

WORKDIR /

VOLUME ["/code","/build/output"]
