FROM ubuntu:xenial
MAINTAINER H. Birch Tree

RUN apt-get -qq update && apt-get -qqy install \
    wget \
    libqt5network5

RUN wget https://github.com/hbirchtree/qthub/releases/download/v1.0.1.1/github-cli -O /github-cli
RUN chmod +x /github-cli

RUN mkdir /data
WORKDIR /data

VOLUME /data
ENTRYPOINT ["/github-cli"]
