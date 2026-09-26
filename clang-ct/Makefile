#
# Clang/LLD toolchain with static LLVM runtimes (compiler-rt, libunwind,
# libc++abi, libc++) for several Linux targets in a single install.
#
# Sysroots are assembled from Debian/Raspbian packages, so resulting binaries
# link glibc dynamically and libc++ statically. glibc baselines:
#   x86_64, aarch64, armv7:  Debian 11 bullseye (2.31, same as Ubuntu 20.04)
#   armv6:                   Raspbian 12 bookworm (2.36)
#
#   make              fetch, build, install, test, package and write manifest
#   make toolchain    build and install into $(PREFIX)
#   make test         compile and run a test program for every target
#   make sysroots     only assemble the sysroots
#   make manifest     write the release manifest read by generate_mega_manifest.py
#

LLVM_VERSION ?= 23.1.2
JOBS ?= $(shell nproc)

DEBIAN_RELEASE   ?= bullseye
RASPBIAN_RELEASE ?= bookworm
DEBIAN_REPO      ?= http://deb.debian.org/debian $(DEBIAN_RELEASE) main
RASPBIAN_REPO    ?= http://raspbian.raspberrypi.com/raspbian $(RASPBIAN_RELEASE) main

# Packages installed into every sysroot; dependencies are resolved automatically.
# Add e.g. libx11-dev or libwayland-dev through SYSROOT_EXTRA_PACKAGES.
SYSROOT_PACKAGES       ?= libc6-dev linux-libc-dev
SYSROOT_EXTRA_PACKAGES ?=

TARGETS := \
	x86_64-unknown-linux-gnu \
	aarch64-unknown-linux-gnu \
	armv7-unknown-linux-gnueabihf \
	armv6-unknown-linux-gnueabihf

TOP        := $(CURDIR)
DOWNLOADS  := $(TOP)/downloads
SRC        := $(TOP)/src/llvm-project-$(LLVM_VERSION).src
BUILD      := $(TOP)/build
PREFIX     := $(TOP)/toolchain
SYSROOTS   := $(PREFIX)/sysroots

# Release naming, following <platform>-ct/<platform>-<arch>_<flavor>.tar.xz
# and <platform>-ct/<arch>.manifest as expected by generate_mega_manifest.py
RELEASE_ARCH     := multi-unknown-linux-gnu
RELEASE_FLAVOR   ?= $(DEBIAN_RELEASE)
BASELINE_TARGET  := x86_64-unknown-linux-gnu
PACKAGE          := $(TOP)/clang-$(RELEASE_ARCH)_$(RELEASE_FLAVOR).tar.xz
MANIFEST         := $(TOP)/$(RELEASE_ARCH).manifest

# Per-target Debian architecture, package repositories, compile flags (kept in
# sync with cmake/Toolchain.cmake) and how to run test binaries
ARCH_x86_64-unknown-linux-gnu        := amd64
ARCH_aarch64-unknown-linux-gnu       := arm64
ARCH_armv7-unknown-linux-gnueabihf  := armhf
ARCH_armv6-unknown-linux-gnueabihf   := armhf

REPOS_x86_64-unknown-linux-gnu       := --repo "$(DEBIAN_REPO)"
REPOS_aarch64-unknown-linux-gnu      := --repo "$(DEBIAN_REPO)"
REPOS_armv7-unknown-linux-gnueabihf := --repo "$(DEBIAN_REPO)"
REPOS_armv6-unknown-linux-gnueabihf  := --repo "$(RASPBIAN_REPO)"

FLAGS_x86_64-unknown-linux-gnu       :=
FLAGS_aarch64-unknown-linux-gnu      :=
FLAGS_armv7-unknown-linux-gnueabihf := -mfloat-abi=hard -mfpu=neon
FLAGS_armv6-unknown-linux-gnueabihf  := -mfloat-abi=hard -mfpu=vfpv2 -marm -mcpu=arm1176jzf-s

RUN_x86_64-unknown-linux-gnu         :=
RUN_aarch64-unknown-linux-gnu        := qemu-aarch64 -L $(SYSROOTS)/aarch64-unknown-linux-gnu
RUN_armv7-unknown-linux-gnueabihf   := qemu-arm -cpu cortex-a7 -L $(SYSROOTS)/armv7-unknown-linux-gnueabihf
RUN_armv6-unknown-linux-gnueabihf    := qemu-arm -cpu arm1176 -L $(SYSROOTS)/armv6-unknown-linux-gnueabihf

LLVM_TARBALL := $(DOWNLOADS)/llvm-project-$(LLVM_VERSION).src.tar.xz
LLVM_URL     := https://github.com/llvm/llvm-project/releases/download/llvmorg-$(LLVM_VERSION)/llvm-project-$(LLVM_VERSION).src.tar.xz

.PHONY: all toolchain sysroots configs test package manifest clean distclean
.DELETE_ON_ERROR:

all: package manifest

SYSROOT_MANIFESTS := $(foreach t,$(TARGETS),$(SYSROOTS)/$(t)/sysroot.manifest)
CONFIGS           := $(foreach t,$(TARGETS),$(PREFIX)/bin/$(t).cfg)

toolchain: $(BUILD)/.installed $(CONFIGS)

sysroots: $(SYSROOT_MANIFESTS)

#
# Sources
#

$(LLVM_TARBALL):
	mkdir -p $(DOWNLOADS)
	wget -O $@ $(LLVM_URL)

$(SRC)/.extracted: $(LLVM_TARBALL)
	mkdir -p $(TOP)/src
	tar -C $(TOP)/src -xf $<
	touch $@

#
# Sysroots
#

$(SYSROOTS)/%/sysroot.manifest: fetch-sysroot.py
	./fetch-sysroot.py \
		$(REPOS_$*) \
		--arch $(ARCH_$*) \
		--cache $(DOWNLOADS)/debs-$(ARCH_$*)-$(if $(findstring armv6,$*),raspbian,debian) \
		--output $(SYSROOTS)/$* \
		$(SYSROOT_PACKAGES) $(SYSROOT_EXTRA_PACKAGES)

#
# LLVM
#

$(BUILD)/build.ninja: $(SRC)/.extracted cmake/Toolchain.cmake
	# Start from a fresh cache so edits to Toolchain.cmake take effect;
	# -D must precede -C for the cache script to see the value
	rm -f $(BUILD)/CMakeCache.txt
	cmake -S $(SRC)/llvm -B $(BUILD) -G Ninja \
		-DCMAKE_INSTALL_PREFIX=$(PREFIX) \
		-DCLANG_CT_SYSROOT_DIR=$(SYSROOTS) \
		-C $(TOP)/cmake/Toolchain.cmake

# Builtins and runtimes are compiled with the just-built clang against the
# sysroots, as part of the install step
$(BUILD)/.installed: $(BUILD)/build.ninja $(SYSROOT_MANIFESTS)
	ninja -C $(BUILD) -j$(JOBS) install
	touch $@

#
# Clang config files: `clang --target=<triple>` (or `<triple>-clang`) picks
# up bin/<triple>.cfg, pointing at the bundled sysroot
#

configs: $(CONFIGS)

$(PREFIX)/bin/%.cfg: $(BUILD)/.installed
	printf '%s\n' \
		'--sysroot=<CFGDIR>/../sysroots/$*' \
		$(FLAGS_$*) \
		'-pthread' \
		> $@
	ln -sf clang   $(PREFIX)/bin/$*-clang
	ln -sf clang++ $(PREFIX)/bin/$*-clang++

#
# Testing
#

test: $(foreach t,$(TARGETS),$(BUILD)/test/.passed-$(t))

$(BUILD)/test/.passed-%: test/hello.cpp $(PREFIX)/bin/%.cfg
	mkdir -p $(BUILD)/test
	$(PREFIX)/bin/clang++ --target=$* -std=c++23 -O2 $< -o $(BUILD)/test/hello-$*
	@echo " * Checking that libc++ is linked statically"
	! readelf -d $(BUILD)/test/hello-$* | grep -E 'NEEDED.*(libc\+\+|libstdc\+\+|libgcc_s|libunwind)'
	readelf -d $(BUILD)/test/hello-$* | grep NEEDED
	$(RUN_$*) $(BUILD)/test/hello-$*
	touch $@

#
# Packaging
#

package: $(PACKAGE)

$(PACKAGE): test
	tar -C $(PREFIX) -c -I 'xz -9 -T0' -f $@ .

#
# Release manifest
#

# Upstream version of a package in a sysroot: $(call deb_version,<triple>,<package>)
deb_version = $$(sed -n 's/^$(2)=\([0-9]*:\)\{0,1\}\([^-+]*\).*/\2/p' $(SYSROOTS)/$(1)/sysroot.manifest)

manifest: $(MANIFEST)

$(MANIFEST): $(SYSROOT_MANIFESTS) Makefile
	{ \
		echo 'CT_LLVM_VERSION="$(LLVM_VERSION)"'; \
		echo "CT_GLIBC_VERSION=\"$(call deb_version,$(BASELINE_TARGET),libc6)\""; \
		echo "CT_LINUX_VERSION=\"$(call deb_version,$(BASELINE_TARGET),linux-libc-dev)\""; \
		echo 'CLANG_CT_TARGETS="$(TARGETS)"'; \
		$(foreach t,$(TARGETS),echo "CLANG_CT_GLIBC_VERSION_$(t)=\"$(call deb_version,$(t),libc6)\""; ) \
	} > $@

clean:
	rm -rf $(BUILD) $(PREFIX) $(PACKAGE) $(MANIFEST)

distclean: clean
	rm -rf $(TOP)/src $(DOWNLOADS)
