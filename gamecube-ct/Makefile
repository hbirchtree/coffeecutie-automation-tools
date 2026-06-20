SOURCE_SHA := devkitPPC_r49
GCC_VER := 15.2.0
NEWLIB_VER := 4.6.0

RELEASE :=

source.tar.gz:
	wget https://github.com/devkitPro/buildscripts/archive/refs/tags/$(SOURCE_SHA).tar.gz -O source.tar.gz

source: source.tar.gz devkitppc-rules-1.2.1.tar.gz config.sh build-devkit.sh
	tar xf source.tar.gz
	mv buildscripts-$(SOURCE_SHA) source
	cp build-devkit.sh source/
	cp config.sh source/
	cp devkitppc-rules-1.2.1.tar.gz source/
	wget https://sourceware.org/pub/binutils/releases/binutils-2.24.tar.bz2 -nc -O source/binutils-2.24.tar.bz2
	wget https://sourceware.org/pub/binutils/releases/binutils-2.45.1.tar.xz -nc -O source/binutils-2.45.1.tar.xz
	wget https://github.com/devkitPro/gcc/archive/refs/tags/devkitPPC_r49.tar.gz -nc -O source/gcc-15.2.0.tar.gz
	wget https://github.com/devkitPro/newlib/archive/refs/heads/devkitPPC_r47.tar.gz -nc -O source/newlib-4.6.0.20260123.tar.gz

build: source
	cd source && INSTALLDIR=$(PWD)/build ./build-devkit.sh

gamecube-powerpc-eabi.manifest: #source
	echo 'CT_GCC_VERSION="15.2.0"\nCT_BINUTILS_VERSION="2.45.1"\nCT_MN_BINUTILS_VERSION="2.24"\nCT_NEWLIB_VERSION="4.6.0"\n' > $(PWD)/gamecube-powerpc-eabi.manifest

gamecube-powerpc-eabi_compiler.tar.xz: #build
	cd $(PWD)/build/devkitPPC && tar Jcf $(PWD)/gamecube-powerpc-eabi_compiler.tar.xz -- *

release: gamecube-powerpc-eabi_compiler.tar.xz gamecube-powerpc-eabi.manifest
	gh release upload $(RELEASE) \
		gamecube-powerpc-eabi.manifest \
		gamecube-powerpc-eabi_compiler.tar.xz

clean:
	rm -r source source.tar.gz build
