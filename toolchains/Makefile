BUILDROOT_VER    ?= 2022.02.1
BUILDROOT_FLAVOR ?=
ARCHITECTURE ?=

%-ct/compiler-bin:
	cd $(PWD)/$*-ct/compiler && CT_PREFIX=$(PWD)/$*-ct/compiler ct-ng build -j4
	ln -s $(PWD)/$*-ct/compiler/$(ARCHITECTURE) $(PWD)/$*-ct/compiler-bin

%-ct/compiler.manifest:
	cd $(PWD)/$*-ct && grep '^CT_GCC_VERSION\|^CT_BINUTILS_VERSION\|^CT_GLIBC_VERSION\|^CT_LINUX_VERSION\|^CT_STRACE_VERSION\|^CT_GDB_VERSION' compiler/.config | grep -v '^#' > $*-$(ARCHITECTURE).manifest
	ln -s $*-$(ARCHITECTURE).manifest $(PWD)/$*-ct/compiler.manifest

%-ct/sysroot:
	cd $*-ct && \
		wget https://buildroot.org/downloads/buildroot-$(BUILDROOT_VER).tar.gz && \
    	tar zxf buildroot-$(BUILDROOT_VER).tar.gz && \
    	mv buildroot-$(BUILDROOT_VER) sysroot && \
		cp $*-ct/sysroot.$(BUILDROOT_FLAVOR).config $*-ct/sysroot/.config

%-ct/.sysroot-completed: $(PWD)/%-ct/sysroot $(PWD)/%-ct/sysroot.config
	cd $(PWD)/$*-ct/sysroot && make prepare-sdk
	touch $(PWD)/$*-ct/.sysroot-completed
	@echo Finished compiler+sysroot bundle

%-ct/compiler-bundle.tar.xz: $(PWD)/%-ct/.sysroot-completed
	cd $(PWD)/$*-ct/sysroot/output/host && \
		tar Jcf $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz -- * && \
		ln -s $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz $(PWD)/$*-ct/compiler-bundle.tar.xz
	@echo Finished compiler+sysroot bundle

%-ct.clean:
	rm -f \
		$(PWD)/$*-ct/compiler-bin \
		$(PWD)/$*-ct/compiler.manifest \
		$(PWD)/$*-ct/.sysroot-completed \
		$(PWD)/$*-ct/compiler-bundle.tar.xz

RELEASE :=

%.upload-release:
	gh release upload "$(RELEASE)" \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE).manifest

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
		-e ARCHITECTURE=x86_64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=multi \
		desktop-ct/compiler-bin \
		desktop-ct/compiler.manifest \
		desktop-ct/compiler-bundle.tar.xz
	@echo Finished target

beaglebone-ct/omap5-sgx-ddk-um-linux:
	git clone https://git.ti.com/git/graphics/omap5-sgx-ddk-um-linux.git beaglebone-ct/omap5-sgx-ddk-um-linux
	git -C beaglebone-ct/omap5-sgx-ddk-um-linux checkout origin/ti-img-sgx/1.14.3699939

SGX_SOURCE_DIR := beaglebone-ct/omap5-sgx-ddk-um-linux/targetfs/ti335x
SGX_DEST_DIR   := beaglebone-ct/sysroot/output/host/arm-buildroot-linux-gnueabihf/sysroot/usr

beaglebone-arm-buildroot-linux-gnueabihf.install-sgx: beaglebone-ct/omap5-sgx-ddk-um-linux
	cp -r $(SGX_SOURCE_DIR)/include/*    $(SGX_DEST_DIR)/include/
	cp    $(SGX_SOURCE_DIR)/lib/lib*.so* $(SGX_DEST_DIR)/lib/
	cp -r $(SGX_SOURCE_DIR)/lib/gbm      $(SGX_DEST_DIR)/lib/

beaglebone-arm-buildroot-linux-gnueabihf.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=wayland \
		-e BUILDROOT_VER=2021.11.3 \
		beaglebone-ct/compiler-bin \
		beaglebone-ct/compiler.manifest \
		beaglebone-ct/.sysroot-completed \
		beaglebone-arm-buildroot-linux-gnueabihf.install-sgx \
		beaglebone-ct/compiler-bundle.tar.xz
	@echo Finished target

#desktop-x86_64-buildroot-linux-gnu.release:
#	make -f $(MAKEFILE_LIST) \
#		-e ARCH=x86_64-buildroot-linux-gnu \
#		-e BUILDROOT_FLAVOR=multi \
#		-e GH_RELEASE=$(GH_RELEASE) \
#		desktop.upload-release
