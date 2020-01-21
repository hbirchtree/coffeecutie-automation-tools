FROM ubuntu:bionic
MAINTAINER H. Birch Tree

ARG CMAKE_VER="3.16"
ARG CMAKE_RELVER="3.16.2"

RUN apt-get -qy update && apt-get -qy install wget

RUN wget https://cmake.org/files/v${CMAKE_VER}/cmake-${CMAKE_RELVER}-Linux-x86_64.sh\
	 -O cmake.sh && \
	chmod +x cmake.sh && \
	./cmake.sh --prefix=/usr/local --skip-license --exclude-subdir && \
    rm cmake.sh

