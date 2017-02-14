FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN dpkg --add-architecture i386

RUN apt-get -qy update && apt-get -qy install \
        build-essential libc6:i386 \
        cmake \
        wget unzip python

RUN adduser coffee

USER coffee
WORKDIR /home/coffee

RUN wget \
    https://storage.googleapis.com/nativeclient-mirror/nacl/nacl_sdk/nacl_sdk.zip
RUN unzip nacl_sdk.zip
RUN $HOME/nacl_sdk/naclsdk update

ENV NACL_ROOT "$HOME/nacl_sdk/pepper_49"

USER root
