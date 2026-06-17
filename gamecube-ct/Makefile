SOURCE_SHA := b5cdec82b7a5e9bcac1b887ad9108855fc81c349
GCC_VER := 15.1.0
NEWLIB_VER := 4.5.0

RELEASE :=

source.tar.gz:
	wget https://github.com/devkitPro/buildscripts/archive/$(SOURCE_SHA).tar.gz -O source.tar.gz

source: source.tar.gz devkitppc-rules-1.2.1.tar.gz config.sh build-devkit.sh
	tar xf source.tar.gz
	mv buildscripts-$(SOURCE_SHA) source
	cp build-devkit.sh source/
	cp config.sh source/
	cp devkitppc-rules-1.2.1.tar.gz source/
	wget https://sourceware.org/pub/binutils/releases/binutils-2.24.tar.bz2 -nc -O source/binutils-2.24.tar.bz2
	wget https://sourceware.org/pub/binutils/releases/binutils-2.44.tar.xz -nc -O source/binutils-2.44.tar.xz
	wget https://github.com/devkitPro/gcc/archive/refs/heads/devkitPPC_r47.tar.gz -nc -O source/gcc-devkitPPC_r47.tar.gz
	wget https://github.com/devkitPro/newlib/archive/refs/heads/devkitPPC_r47.tar.gz -nc -O source/newlib-devkitPPC_r47.tar.gz

build: source
	cd source && INSTALLDIR=$(PWD)/build ./build-devkit.sh

gamecube-powerpc-eabi.manifest: #source
	echo 'CT_GCC_VERSION="15.1.0"\nCT_BINUTILS_VERSION="2.44"\nCT_MN_BINUTILS_VERSION="2.44"\nCT_NEWLIB_VERSION="4.5.0"\n' > $(PWD)/gamecube-powerpc-eabi.manifest

gamecube-powerpc-eabi_compiler.tar.xz: #build
	cd $(PWD)/build/devkitPPC && tar Jcf $(PWD)/gamecube-powerpc-eabi_compiler.tar.xz -- *

release: gamecube-powerpc-eabi_compiler.tar.xz gamecube-powerpc-eabi.manifest
	gh release upload $(RELEASE) \
		gamecube-powerpc-eabi.manifest \
		gamecube-powerpc-eabi_compiler.tar.xz

clean:
	rm -r source
