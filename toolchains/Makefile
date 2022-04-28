BUILDROOT_VER    ?= 2022.02.1
BUILDROOT_FLAVOR ?=
ARCH ?=

%-ct/compiler-bin:
	cd $(PWD)/$*-ct/compiler && CT_PREFIX=$(PWD)/$*-ct/compiler ct-ng build -j4
	ln -s $(PWD)/$*-ct/compiler/$(ARCH) $(PWD)/$*-ct/compiler-bin

%-ct/compiler.manifest:
	cd $(PWD)/$*-ct && grep '^CT_GCC_VERSION\|^CT_BINUTILS_VERSION\|^CT_GLIBC_VERSION\|^CT_LINUX_VERSION\|^CT_STRACE_VERSION\|^CT_GDB_VERSION' compiler/.config | grep -v '^#' > $*-$(ARCH).manifest
	ln -s $(PWD)/$*-ct/$*-$(ARCH).manifest $(PWD)/$*-ct/compiler.manifest

%-ct/sysroot:
	cd $*-ct && \
	wget https://buildroot.org/downloads/buildroot-$(BUILDROOT_VER).tar.gz && \
    tar zxf buildroot-$(BUILDROOT_VER).tar.gz && \
    mv buildroot-$(BUILDROOT_VER) $*-ct/sysroot && \
	cp $*-ct/sysroot.$(BUILDROOT_FLAVOR).config $*-ct/sysroot/.config

%-ct/compiler-bundle.tar.xz: $(PWD)/%-ct/sysroot $(PWD)/%-ct/sysroot.config
	cd $(PWD)/$*-ct && \
		cd $(PWD)/$*-ct/sysroot && \
		make sdk && \
		cd $(PWD)/$*-ct/sysroot/output/host && \
		tar Jcf $(PWD)/$*-ct/$*-$(ARCH)_$(BUILDROOT_FLAVOR).tar.xz -- * && \
		ln -s $(PWD)/$*-ct/$*-$(ARCH)_$(BUILDROOT_FLAVOR).tar.xz $(PWD)/$*-ct/compiler-bundle.tar.xz
	@echo Finished compiler+sysroot bundle

RELEASE :=

%.upload-release:
	gh release upload "$(RELEASE)" $(PWD)/$*-ct/$*-$(ARCH)_$(BUILDROOT_FLAVOR).tar.xz $(PWD)/$*-ct/$*-$(ARCH).manifest

DEBROOT_ARCH    :=
DEBROOT_SUITE   := stretch

%.debroot:
	cd $(PWD)/$*-ct && \
		sudo qemu-debootstrap \
			--include=$(shell cat $(PWD)/$*-ct/packages.list) \
			--exclude=libstdc++6 \
			--arch=$(DEBROOT_ARCH) \
			$(DEBROOT_SUITE) \
			sysroot

#toolchain.build:
#	make -f $(MAKEFILE_LIST) \
#		-e ARCH=$(ARCH) \
#		-e BUILDROOT_FLAVOR=$(FLAVOR) \
#		$(TOOLCHAIN).compiler \
#		$(TOOLCHAIN).compiler-manifest \
#		$(TOOLCHAIN).buildroot
#	@echo Finished target

#toolchain.release:
#	make -f $(MAKEFILE_LIST) \
#		-e ARCH=$(ARCH) \
#		-e BUILDROOT_FLAVOR=$(FLAVOR) \
#		-e GH_RELEASE=$(GH_RELEASE) \
#		$(TOOLCHAIN).upload-release

desktop-x86_64-buildroot-linux-gnu.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCH=x86_64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=multi \
		desktop-ct/compiler-bin \
		desktop-ct/compiler.manifest \
		desktop-ct/compiler-bundle.tar.xz
	@echo Finished target

beaglebone-arm-buildroot-linux-gnueabihf.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCH=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=wayland \
		-e BUILDROOT_VER=2021.11.3 \
		beaglebone-ct/compiler-bin \
		beaglebone-ct/compiler.manifest \
		beaglebone-ct/compiler-bundle.tar.xz
	@echo Finished target

#desktop-x86_64-buildroot-linux-gnu.release:
#	make -f $(MAKEFILE_LIST) \
#		-e ARCH=x86_64-buildroot-linux-gnu \
#		-e BUILDROOT_FLAVOR=multi \
#		-e GH_RELEASE=$(GH_RELEASE) \
#		desktop.upload-release
