FROM alpine:latest
MAINTAINER H. Birch Tree

ENV LD_LIBRARY_PATH "/opt/gcc-6.1/lib"

VOLUME ["/maemo","/maemo/source","/maemo/build"]
CMD ["bash"]

ENTRYPOINT ["chroot", "/maemo"]
