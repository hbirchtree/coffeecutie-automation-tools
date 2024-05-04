
#
# Complete targets
#

desktop-x86_64-buildroot-linux-gnu.build:
	make -f Makefile.common \
		-e ARCHITECTURE=x86_64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=multi \
		-e BUILDROOT_VER=2022.11 \
		desktop-ct/.sysroot-x86_64-buildroot-linux-gnu-multi-completed \
		desktop-ct/.x86_64-buildroot-linux-gnu-multi-compiler-bundle \
		desktop-ct/.x86_64-buildroot-linux-gnu-multi-target-bundle
	@echo Finished target

desktop-arm-buildroot-linux-gnueabihf-wayland.build:
	make -f Makefile.common \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=arm-wayland \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/.sysroot-arm-buildroot-linux-gnueabihf-arm-wayland-completed \
		desktop-ct/.arm-buildroot-linux-gnueabihf-arm-wayland-compiler-bundle \
		desktop-ct/.arm-buildroot-linux-gnueabihf-arm-wayland-target-bundle
	@echo Finished target

desktop-armv6-buildroot-linux-gnueabihf-wayland.build:
	make -f Makefile.common \
		-e ARCHITECTURE=armv6-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=armv6-wayland \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/.sysroot-armv6-buildroot-linux-gnueabihf-armv6-wayland-completed \
		desktop-ct/.armv6-buildroot-linux-gnueabihf-armv6-wayland-compiler-bundle \
		desktop-ct/.armv6-buildroot-linux-gnueabihf-armv6-wayland-target-bundle
	@echo Finished target

desktop-aarch64-buildroot-linux-gnu-multi.build:
	make -f Makefile.common \
		-e ARCHITECTURE=aarch64-buildroot-linux-gnu \
		-e BUILDROOT_FLAVOR=aarch64-multi \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/.sysroot-aarch64-buildroot-linux-gnu-aarch64-multi-completed \
		desktop-ct/.aarch64-buildroot-linux-gnu-aarch64-multi-compiler-bundle \
		desktop-ct/.aarch64-buildroot-linux-gnu-aarch64-multi-target-bundle
	@echo Finished target

#
# Derived targets; add some extra libraries to an existing sysroot
#
	
beaglebone-arm-buildroot-linux-gnueabihf.build:
	make -f Makefile.common \
		-e ARCHITECTURE=arm-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=arm-wayland \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/.sysroot-arm-buildroot-linux-gnueabihf-arm-wayland-completed
	make -f Makefile.common \
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

raspberry-armv6-buildroot-linux-gnueabihf.build:
	make -f Makefile.common \
		-e ARCHITECTURE=armv6-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=armv6-wayland \
		-e BUILDROOT_VER=2023.08.1 \
		desktop-ct/.sysroot-armv6-buildroot-linux-gnueabihf-armv6-wayland-completed
	make -f Makefile.common \
		-e ARCHITECTURE=armv6-buildroot-linux-gnueabihf \
		-e BUILDROOT_FLAVOR=vc \
		-e BUILDROOT_VER=2023.08.1 \
		-e BUILDROOT_RENAME_FROM=armv6-buildroot-linux-gnueabihf \
		-e SOURCE_PLATFORM=desktop \
		-e SOURCE_FLAVOR=arm-wayland \
		raspberry-ct/sysroot-armv6-buildroot-linux-gnueabihf-vc/output/host \
		raspberry-ct/sysroot-armv6-buildroot-linux-gnueabihf-vc/output/target \
		raspberry-ct.install-vc \
		raspberry-ct/.armv6-buildroot-linux-gnueabihf-vc-compiler-bundle \
		raspberry-ct/.armv6-buildroot-linux-gnueabihf-vc-target-bundle
	@echo Finished target

desktop-x86_64-w64-mingw32.build:
	windows-ct/build-mingw64.sh
	cd windows-ct/mingw-w64 && \
		tar -c -I 'xz -9 -T0' -f ../windows-x86_64-w64-mingw32.tar.xz *

#
# The big stuff
#

clean-all:
	rm \
		*-ct/*.tar.xz \
		*-ct/*.manifest \
		*-ct/.sysroot-* \
		*-ct/.*-compiler-bundle \
		*-ct/.*-target-bundle

meta.json:
	./generate_mega_manifest.py | jq > meta.json
	@echo " * Run ./create_staging.sh to proceed"

all:
	make -f Makefile.common \
		desktop-aarch64-buildroot-linux-gnu-multi.build \
		desktop-arm-buildroot-linux-gnueabihf-wayland.build \
		desktop-x86_64-buildroot-linux-gnu.build \
		beaglebone-arm-buildroot-linux-gnueabihf.build \
		raspberry-armv6-buildroot-linux-gnueabihf.build
	make meta.json
