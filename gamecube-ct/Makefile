SOURCE_SHA := cc0e589e56c745d5d08ee3d3297f82a511600aa2
GCC_VER := 13.1
NEWLIB_VER := 4.3

RELEASE :=

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

gamecube-powerpc-eabi.manifest: #source
	echo "CT_GCC_VERSION=13.1.0\nCT_BINUTILS_VERSION=2.40.0\nCT_NEWLIB_VERSION=4.3.0\n" > $(PWD)/gamecube-powerpc-eabi.manifest

gamecube-powerpc-eabi_compiler.tar.xz: #build
	cd $(PWD)/build/devkitPPC && tar Jcf $(PWD)/gamecube-powerpc-eabi_compiler.tar.xz -- *

release: gamecube-powerpc-eabi_compiler.tar.xz gamecube-powerpc-eabi.manifest
	gh release upload $(RELEASE) \
		gamecube-powerpc-eabi.manifest \
		gamecube-powerpc-eabi_compiler.tar.xz

clean:
	rm -r source
