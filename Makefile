BUILDROOT_VER    ?= 2022.02.7
BUILDROOT_FLAVOR ?=
ARCHITECTURE ?=
GDB_VERSION ?= 12.1

.PHONY: meta.json

.PRECIOUS: %-ct/compiler-$(ARCHITECTURE)/$(ARCHITECTURE) %-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR) %-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/compiler

%-ct/compiler-$(ARCHITECTURE)/$(ARCHITECTURE): %-ct/compiler-$(ARCHITECTURE)/.config
	cd $(PWD)/$*-ct/compiler-$(ARCHITECTURE) && CT_PREFIX=$(PWD)/$*-ct/compiler-$(ARCHITECTURE) ct-ng build -j4

%-ct/$(ARCHITECTURE).manifest: %-ct/compiler-$(ARCHITECTURE)/.config
	cd $(PWD)/$*-ct && \
		grep '^CT_GCC_VERSION\|^CT_BINUTILS_VERSION\|^CT_GLIBC_VERSION\|^CT_LINUX_VERSION\|^CT_STRACE_VERSION\|^CT_GDB_VERSION' \
		compiler-$(ARCHITECTURE)/.config | \
			grep -v '^#' > $(ARCHITECTURE).manifest

%-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR):
	cd $*-ct && \
		wget https://buildroot.org/downloads/buildroot-$(BUILDROOT_VER).tar.gz && \
    	tar zxf buildroot-$(BUILDROOT_VER).tar.gz && \
		rm buildroot-$(BUILDROOT_VER).tar.gz && \
    	mv buildroot-$(BUILDROOT_VER) sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR) && \
		cp sysroot.$(BUILDROOT_FLAVOR).config sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/.config

%-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/compiler:
	mkdir -p $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host
	cp -r --no-preserve=ownership \
		$*-ct/compiler-$(ARCHITECTURE)/$(ARCHITECTURE) \
		$*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/compiler
	chmod -R +w $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/compiler

%-ct/.sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-completed: %-ct/compiler-$(ARCHITECTURE)/$(ARCHITECTURE) %-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR) %-ct/sysroot.$(BUILDROOT_FLAVOR).config %-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/compiler
	cd $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR) && make prepare-sdk
	touch $*-ct/.sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-completed
	@echo Finished compiler+sysroot bundle

%-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/$(ARCHITECTURE):
	mv $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/$(BUILDROOT_RENAME_FROM) $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/$(ARCHITECTURE)

%-ct/.$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-compiler-bundle: %-ct/.sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-completed
	cd $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host && \
		env XZ_DEFAULT="-T0" tar Jcf $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz -- *
	touch $*-ct/.$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-compiler-bundle
	@echo Finished compiler+sysroot bundle

%-ct/.$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-target-bundle: %-ct/.sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-completed
	cd $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/target && \
		env XZ_DEFAULT="-T0" tar Jcf $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR)_target.tar.xz -- \
			usr/lib \
			usr/share \
			lib
	touch $*-ct/.$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-target-bundle
	@echo Finished compiler+sysroot bundle

%-ct.clean:
	rm -f \
		$(PWD)/$*-ct/$(ARCHITECTURE).manifest \
		$(PWD)/$*-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed \
		$(PWD)/$*-ct/$(BUILDROOT_FLAVOR)-compiler-bundle.tar.xz
		#$(PWD)/$*-ct/compiler-bin \

# Source platform directory + flavor to copy
# Architecture is assumed to be the same because... Duh
SOURCE_PLATFORM :=
SOURCE_FLAVOR :=

%-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host: $(SOURCE_PLATFORM)-ct/.sysroot-$(ARCHITECTURE)-$(SOURCE_FLAVOR)-completed
	mkdir -p $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output
	cp -r --no-preserve=ownership \
		$(SOURCE_PLATFORM)-ct/sysroot-$(ARCHITECTURE)-$(SOURCE_FLAVOR)/output/host \
		$*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/
	chmod -R +w $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host
	cp $(SOURCE_PLATFORM)-ct/$(ARCHITECTURE).manifest $*-ct/$(ARCHITECTURE).manifest
	touch $*-ct/.sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-completed

%-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/target: $(SOURCE_PLATFORM)-ct/.sysroot-$(ARCHITECTURE)-$(SOURCE_FLAVOR)-completed
	mkdir -p $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output
	cp -r --no-preserve=ownership \
		$(SOURCE_PLATFORM)-ct/sysroot-$(ARCHITECTURE)-$(SOURCE_FLAVOR)/output/target \
		$*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/
	chmod -R +w $*-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/target
	touch $*-ct/.sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)-completed

RELEASE :=

release: .PHONY
	gh release create $(RELEASE)

%.upload-release:
	gh release upload "$(RELEASE)" --clobber \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR)_target.tar.xz \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE).manifest

%-ct/gdb-src:
	wget https://ftp.gnu.org/gnu/gdb/gdb-$(GDB_VERSION).tar.xz -O $(PWD)/$*-ct/gdb.tar.xz
	cd $(PWD)/$*-ct && tar xf $(PWD)/$*-ct/gdb.tar.xz
	mv $(PWD)/$*-ct/gdb-$(GDB_VERSION) $(PWD)/$*-ct/gdb-src

%-ct/.gdb-python-completed: %-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed %-ct/gdb-src
	cd $(PWD)/$*-ct/gdb-src && \
		PATH=$(PWD)/$*-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host/bin:$(PATH) && \
		./configure \
			--target=arm-buildroot-linux-gnueabihf \
			--program-prefix=arm-buildroot-linux-gnueabihf- \
			--prefix=$(PWD)/$*-ct/host-gdb \
			--with-python && \
		make -j`nproc` && \
		make install
	cd $(PWD)/$*-ct/gdb-src/gdbserver && \
		./configure \
			--host=arm-buildroot-linux-gnueabihf \
			--target=arm-buildroot-linux-gnueabihf \
			--prefix=$(PWD)/$*-ct/host-gdb/gdbserver \
			--with-python && \
		make -j`nproc` && \
		make install
	@echo '-- gdb and gdbserver built'

desktop-x86_64-buildroot-linux-gnu.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=x86_64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=multi \
		-e BUILDROOT_VER=2022.11 \
		desktop-ct/compiler-x86_64-buildroot-linux-gnu/x86_64-buildroot-linux-gnu \
		desktop-ct/x86_64-buildroot-linux-gnu.manifest \
		desktop-ct/.x86_64-buildroot-linux-gnu-multi-compiler-bundle \
		desktop-ct/.x86_64-buildroot-linux-gnu-multi-target-bundle
	@echo Finished target

beaglebone-ct/omap5-sgx-ddk-um-linux:
	git clone https://git.ti.com/git/graphics/omap5-sgx-ddk-um-linux.git beaglebone-ct/omap5-sgx-ddk-um-linux
	git -C beaglebone-ct/omap5-sgx-ddk-um-linux checkout origin/ti-img-sgx/1.14.3699939

SGX_SOURCE_DIR := beaglebone-ct/omap5-sgx-ddk-um-linux/targetfs/ti335x
SGX_DEST_DIR   := beaglebone-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/arm-buildroot-linux-gnueabihf/sysroot/usr

beaglebone-ct.install-sgx: beaglebone-ct/omap5-sgx-ddk-um-linux
	cp -r $(SGX_SOURCE_DIR)/include/*    $(SGX_DEST_DIR)/include/
	cp    $(SGX_SOURCE_DIR)/lib/lib*.so* $(SGX_DEST_DIR)/lib/
	#cp -r $(SGX_SOURCE_DIR)/lib/gbm      $(SGX_DEST_DIR)/lib/

beaglebone-arm-buildroot-linux-gnueabihf.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=ti-sgx \
		-e BUILDROOT_VER=2023.08.1 \
		-e SOURCE_PLATFORM=desktop \
		-e SOURCE_FLAVOR=arm-wayland \
		beaglebone-ct/sysroot-arm-buildroot-linux-gnueabihf-ti-sgx/output/host \
		beaglebone-ct/sysroot-arm-buildroot-linux-gnueabihf-ti-sgx/output/target \
		beaglebone-ct.install-sgx \
		beaglebone-ct/.arm-buildroot-linux-gnueabihf-ti-sgx-compiler-bundle \
		beaglebone-ct/.arm-buildroot-linux-gnueabihf-ti-sgx-target-bundle
	@echo Finished target

desktop-arm-buildroot-linux-gnueabihf-wayland.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=arm-wayland \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/arm-buildroot-linux-gnueabihf.manifest \
		desktop-ct/.arm-buildroot-linux-gnueabihf-arm-wayland-compiler-bundle \
		desktop-ct/.arm-buildroot-linux-gnueabihf-arm-wayland-target-bundle
	@echo Finished target

desktop-armv6-buildroot-linux-gnueabihf-wayland.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=armv6-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=arm-wayland \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/armv6-buildroot-linux-gnueabihf.manifest \
		desktop-ct/.sysroot-armv6-buildroot-linux-gnueabihf-arm-wayland-completed
	@echo Finished target

desktop-aarch64-buildroot-linux-gnu-multi.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=aarch64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=aarch64-multi \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/aarch64-buildroot-linux-gnu.manifest \
		desktop-ct/.aarch64-buildroot-linux-gnu-aarch64-multi-compiler-bundle \
		desktop-ct/.aarch64-buildroot-linux-gnu-aarch64-multi-target-bundle
	@echo Finished target

raspberry-ct/rpi-firmware:
	git clone https://github.com/raspberrypi/rpi-firmware.git raspberry-ct/rpi-firmware --branch stable --depth 1

VC_SOURCE_DIR := raspberry-ct/rpi-firmware/vc
VC_DEST_DIR   := raspberry-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/host/$(ARCHITECTURE)/sysroot/usr
VC_TARGET_DEST_DIR   := raspberry-ct/sysroot-$(ARCHITECTURE)-$(BUILDROOT_FLAVOR)/output/target/usr

raspberry-ct.install-vc: raspberry-ct/rpi-firmware
	cp -r $(VC_SOURCE_DIR)/sdk/opt/vc/include/*          $(VC_DEST_DIR)/include
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/*.a         $(VC_DEST_DIR)/lib
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/*.so*       $(VC_DEST_DIR)/lib
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/pkgconfig/* $(VC_DEST_DIR)/lib/pkgconfig
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/*.so*       $(VC_TARGET_DEST_DIR)/lib

raspberry-armv6-buildroot-linux-gnueabihf.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=armv6-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=vc \
		-e BUILDROOT_VER=2023.08.1 \
		-e BUILDROOT_RENAME_FROM=arm-buildroot-linux-gnueabihf \
		-e SOURCE_PLATFORM=desktop \
		-e SOURCE_FLAVOR=arm-wayland \
		raspberry-ct/sysroot-armv6-buildroot-linux-gnueabihf-vc/output/host \
		raspberry-ct/sysroot-armv6-buildroot-linux-gnueabihf-vc/output/host/armv6-buildroot-linux-gnueabihf \
		raspberry-ct/sysroot-armv6-buildroot-linux-gnueabihf-vc/output/target \
		raspberry-ct.install-vc \
		raspberry-ct/.armv6-buildroot-linux-gnueabihf-vc-compiler-bundle \
		raspberry-ct/.armv6-buildroot-linux-gnueabihf-vc-target-bundle
	@echo Finished target


meta.json:
	./generate_mega_manifest.py | jq > meta.json

