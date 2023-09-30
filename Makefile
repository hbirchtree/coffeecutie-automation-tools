BUILDROOT_VER    ?= 2022.02.7
BUILDROOT_FLAVOR ?=
ARCHITECTURE ?=
GDB_VERSION ?= 12.1

%-ct/compiler-bin:
	cd $(PWD)/$*-ct/compiler && CT_PREFIX=$(PWD)/$*-ct/compiler ct-ng build -j4
	ln -s $(PWD)/$*-ct/compiler/$(ARCHITECTURE) $(PWD)/$*-ct/compiler-bin

%-ct/compiler.manifest:
	cd $(PWD)/$*-ct && grep '^CT_GCC_VERSION\|^CT_BINUTILS_VERSION\|^CT_GLIBC_VERSION\|^CT_LINUX_VERSION\|^CT_STRACE_VERSION\|^CT_GDB_VERSION' compiler/.config | grep -v '^#' > $*-$(ARCHITECTURE).manifest
	ln -s $*-$(ARCHITECTURE).manifest $(PWD)/$*-ct/compiler.manifest

%-ct/sysroot-$(BUILDROOT_FLAVOR):
	cd $*-ct && \
		wget https://buildroot.org/downloads/buildroot-$(BUILDROOT_VER).tar.gz && \
    	tar zxf buildroot-$(BUILDROOT_VER).tar.gz && \
    	mv buildroot-$(BUILDROOT_VER) sysroot-$(BUILDROOT_FLAVOR) && \
		cp $*-ct/sysroot.$(BUILDROOT_FLAVOR).config $*-ct/sysroot-$(BUILDROOT_FLAVOR)/.config

%-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed: $(PWD)/%-ct/sysroot-$(BUILDROOT_FLAVOR) %-ct/sysroot.$(BUILDROOT_FLAVOR).config
	mkdir -p $*-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host
	cp -r $*-ct/compiler/$(ARCHITECTURE) $*-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host/compiler
	cd $(PWD)/$*-ct/sysroot-$(BUILDROOT_FLAVOR) && make prepare-sdk
	touch $(PWD)/$*-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed
	@echo Finished compiler+sysroot bundle

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

%-ct/$(BUILDROOT_FLAVOR)-compiler-bundle.tar.xz: %-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed
	cd $(PWD)/$*-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host && \
		tar Jcf $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz -- * && \
		ln -s $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz $(PWD)/$*-ct/$(BUILDROOT_FLAVOR)-compiler-bundle.tar.xz
	@echo Finished compiler+sysroot bundle

%-ct/.$(BUILDROOT_FLAVOR)-target-bundle: %-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed
	cd $(PWD)/$*-ct/sysroot-$(BUILDROOT_FLAVOR)/output/target && \
		tar Jcf $(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR)_target.tar.xz -- \
			usr/lib \
			usr/share \
			lib \
	   	&& \
		cd ../../../ && \
		touch .$(BUILDROOT_FLAVOR)-target-bundle
	@echo Finished compiler+sysroot bundle

%-ct.clean:
	rm -f \
		$(PWD)/$*-ct/compiler-bin \
		$(PWD)/$*-ct/compiler.manifest \
		$(PWD)/$*-ct/.sysroot-$(BUILDROOT_FLAVOR)-completed \
		$(PWD)/$*-ct/$(BUILDROOT_FLAVOR)-compiler-bundle.tar.xz

RELEASE :=

release:
	gh release create $(RELEASE)

%.upload-release:
	gh release upload "$(RELEASE)" --clobber \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR).tar.xz \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE)_$(BUILDROOT_FLAVOR)_target.tar.xz \
		$(PWD)/$*-ct/$*-$(ARCHITECTURE).manifest

desktop-x86_64-buildroot-linux-gnu.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=x86_64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=multi \
		-e BUILDROOT_VER=2022.11 \
		desktop-ct/compiler-bin \
		desktop-ct/compiler.manifest \
		desktop-ct/multi-compiler-bundle.tar.xz \
		desktop-ct/.multi-target-bundle
	@echo Finished target

beaglebone-ct/omap5-sgx-ddk-um-linux:
	git clone https://git.ti.com/git/graphics/omap5-sgx-ddk-um-linux.git beaglebone-ct/omap5-sgx-ddk-um-linux
	git -C beaglebone-ct/omap5-sgx-ddk-um-linux checkout origin/ti-img-sgx/1.14.3699939

SGX_SOURCE_DIR := beaglebone-ct/omap5-sgx-ddk-um-linux/targetfs/ti335x
SGX_DEST_DIR   := beaglebone-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host/arm-buildroot-linux-gnueabihf/sysroot/usr

beaglebone-ct.install-sgx: beaglebone-ct/omap5-sgx-ddk-um-linux
	cp -r $(SGX_SOURCE_DIR)/include/*    $(SGX_DEST_DIR)/include/
	cp    $(SGX_SOURCE_DIR)/lib/lib*.so* $(SGX_DEST_DIR)/lib/
	#cp -r $(SGX_SOURCE_DIR)/lib/gbm      $(SGX_DEST_DIR)/lib/

beaglebone-arm-buildroot-linux-gnueabihf.clean:
	rm -f \
		beaglebone-ct/ti-sgx-compiler-bundle.tar.xz \
		beaglebone-ct/*_ti-sgx.tar.xz \
		beaglebone-ct/.sysroot-ti-sgx-completed \
		beaglebone-ct/compiler-bin \
		beaglebone-ct/compiler.manifest \
		beaglebone-ct/.ti-sgx-target-bundle
	rm -rf \
		beaglebone-ct/omap5-sgx-ddk-um-linux
		beaglebone-ct/sysroot-ti-sgx \
		beaglebone-ct/compiler/.build \
		beaglebone-ct/compiler/arm-buildroot-linux-gnueabihf

beaglebone-arm-buildroot-linux-gnueabihf.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=ti-sgx \
		-e BUILDROOT_VER=2022.11 \
		beaglebone-ct/compiler-bin \
		beaglebone-ct/compiler.manifest \
		beaglebone-ct/.sysroot-ti-sgx-completed \
		beaglebone-ct.install-sgx \
		beaglebone-ct/ti-sgx-compiler-bundle.tar.xz \
		beaglebone-ct/.ti-sgx-target-bundle
	@echo Finished target
		#beaglebone-ct/.gdb-python-completed \

generic-arm-buildroot-linux-gnueabihf-wayland.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=wayland \
		-e BUILDROOT_VER=2022.11 \
		arm-wayland-ct/compiler-bin \
		arm-wayland-ct/compiler.manifest \
		arm-wayland-ct/wayland-compiler-bundle.tar.xz \
		arm-wayland-ct/.wayland-target-bundle
	@echo Finished target
		#arm-wayland-ct/.gdb-python-completed


raspberry-ct/rpi-firmware:
	git clone https://github.com/raspberrypi/rpi-firmware.git raspberry-ct/rpi-firmware --branch stable --depth 1

VC_SOURCE_DIR := raspberry-ct/rpi-firmware/vc
VC_DEST_DIR   := raspberry-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host/arm-buildroot-linux-gnueabihf/sysroot/usr
VC_TARGET_DEST_DIR   := raspberry-ct/sysroot-$(BUILDROOT_FLAVOR)/output/target/usr

raspberry-ct.install-vc: raspberry-ct/rpi-firmware
	cp -r $(VC_SOURCE_DIR)/sdk/opt/vc/include/*          $(VC_DEST_DIR)/include
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/*.a         $(VC_DEST_DIR)/lib
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/*.so*       $(VC_DEST_DIR)/lib
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/pkgconfig/* $(VC_DEST_DIR)/lib/pkgconfig
	cp    $(VC_SOURCE_DIR)/hardfp/opt/vc/lib/*.so*       $(VC_TARGET_DEST_DIR)/lib

raspberry-arm-buildroot-linux-gnueabihf.build:
	make -f $(MAKEFILE_LIST) \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=vc \
		-e BUILDROOT_VER=2022.11 \
		raspberry-ct/compiler-bin \
		raspberry-ct/compiler.manifest \
		raspberry-ct/.sysroot-vc-completed \
		raspberry-ct.install-vc \
		raspberry-ct/vc-compiler-bundle.tar.xz \
		raspberry-ct/.vc-target-bundle
	@echo Finished target

