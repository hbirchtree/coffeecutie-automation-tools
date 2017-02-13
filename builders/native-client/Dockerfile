FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN dpkg --add-architecture i386

RUN apt-get -qy update && apt-get -qy install \
        build-essential libc6:i386 \
        wget unzip python

RUN wget \
    https://storage.googleapis.com/nativeclient-mirror/nacl/nacl_sdk/nacl_sdk.zip
RUN unzip nacl_sdk.zip

RUN /nacl_sdk/naclsdk update
