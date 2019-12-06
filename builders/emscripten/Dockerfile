#
# This Dockerfile sets up an environment for building with emscripten
#

FROM hbirch/ubuntu-cmake:3.16
MAINTAINER H. Birch Tree

ENV USNAME="coffee"
ENV EMSDK_DIR="/home/coffee/emsdk_portable"

# Installing some dependencies
RUN apt-get -qy update && apt-get -qy install \
    wget build-essential git ninja-build \
    libxml2 \
    python3 python3-distutils \
    curl

# Get the emsdk
RUN ln -s /usr/bin/python3 /usr/bin/python

RUN adduser -u 1000 $USNAME
USER $USNAME

WORKDIR /home/$USNAME
RUN wget https://github.com/emscripten-core/emsdk/archive/master.tar.gz \
    && tar -zxvf master.tar.gz \
    && rm master.tar.gz \
    && [ -d emsdk-master ] && mv emsdk-master $EMSDK_DIR

# Installing latest Emscripten versio
RUN $EMSDK_DIR/emsdk update
RUN $EMSDK_DIR/emsdk install latest

# Set up environment variables
ENV PATH=$PATH:$EMSDK_DIR:$EMSDK_DIR/clang/fastcomp/build_master_64/bin:$EMSDK_DIR/node/4.1.1_64bit/bin:$EMSDK_DIR/emscripten/master
ENV EMSCRIPTEN_ROOT=$EMSDK_DIR/emscripten/master

RUN $EMSDK_DIR/emsdk activate latest

WORKDIR /

VOLUME ["/code","/build/output"]
