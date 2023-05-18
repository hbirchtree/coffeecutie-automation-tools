SOURCE_SHA := cc0e589e56c745d5d08ee3d3297f82a511600aa2
GCC_VER := 13.1
NEWLIB_VER := 4.3

source:
	wget https://github.com/devkitPro/buildscripts/archive/$(SOURCE_SHA).tar.gz -O source.tar.gz
	tar xf source.tar.gz
	mv buildscripts-$(SOURCE_SHA) source
	cp build-devkit.sh source/
	cp config.sh source/
	cp devkitppc-rules-1.1.2.tar.gz source/
	rm source.tar.gz

build: source
	cd source && BUILD_DKPRO_INSTALLDIR=$(PWD)/build ./build-devkit.sh

package: build
	cd $(PWD)/build/devkitPPC && tar Jcf $(PWD)/gamecube-powerpc-eabi-gcc-$(GCC_VER)-newlib-$(NEWLIB_VER).tar.xz -- *

clean:
	rm -r source
