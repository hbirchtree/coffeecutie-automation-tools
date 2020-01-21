#
# This Dockerfile will create a stock compilation environment on Ubuntu LTS
# The listed dependencies might aid in finding dependencies for other distributions
#

FROM hbirch/ubuntu-cmake:latest
MAINTAINER H. Birch Tree

ENV USER coffee
ENV USER_ID 1000

ENV PROJECT_VOLUME "/home/${USER}/project"
ENV BUILD_VOLUME "/home/${USER}/build"

RUN apt-get -qy update && apt-get -qy install \
        build-essential \
        doxygen graphviz \
        libqt5network5 \
        wget git lsb-release \
	curl

#Create user directory + group
RUN groupadd coffeeusers -g 1000
RUN useradd coffee -m -u 1000

USER coffee
RUN mkdir -p ${PROJECT_VOLUME} ${BUILD_VOLUME} ${BUILD_VOLUME}/out

WORKDIR ${BUILD_VOLUME}

VOLUME ["${PROJECT_VOLUME}","${BUILD_VOLUME}","${BUILD_VOLUME}/out"]
