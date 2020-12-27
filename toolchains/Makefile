%.compiler:
	cd $(PWD)/$*-ct/compiler && ct-ng build -j4

BUILDROOT_VER := 2020.11

%.buildroot:
	cd $(PWD)/$*-ct && \
		wget https://buildroot.org/downloads/buildroot-$(BUILDROOT_VER).tar.gz && \
		tar zxf buildroot-$(BUILDROOT_VER).tar.gz && \
		mv buildroot-$(BUILDROOT_VER) $(PWD)/$*-ct/sysroot && \
		cd $(PWD)/$*-ct/sysroot && \
		cp $(PWD)/$*-ct/sysroot.config $(PWD)/$*-ct/sysroot/.config && \
		make

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

