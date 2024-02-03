%-ct/gdb-src:
	wget https://ftp.gnu.org/gnu/gdb/gdb-$(GDB_VERSION).tar.xz -O $(PWD)/$*-ct/gdb.tar.xz
	cd $(PWD)/$*-ct && tar xf $(PWD)/$*-ct/gdb.tar.xz
	mv $(PWD)/$*-ct/gdb-$(GDB_VERSION) $(PWD)/$*-ct/gdb-src

%-ct/.gdb-python-completed: %-ct/gdb-src
	cd $(PWD)/$*-ct/gdb-src && \
		PATH=$(PWD)/$*-ct/sysroot-$(BUILDROOT_FLAVOR)/output/host/bin:$(PATH) && \
		./configure \
			--target=$(ARCHITECTIRE) \
			--program-prefix=$(ARCHITECTURE)- \
			--prefix=$(PWD)/$*-ct/host-gdb-$(ARCHITECTURE) \
			--with-python && \
		make -j`nproc` && \
		make install
	cd $(PWD)/$*-ct/gdb-src/gdbserver && \
		./configure \
			--host=$(ARCHITECTURE) \
			--target=$(ARCHITECTURE) \
			--prefix=$(PWD)/$*-ct/host-gdb-$(ARCHITECTURE)/gdbserver \
			--with-python && \
		make -j`nproc` && \
		make install
	@echo '-- gdb and gdbserver built'
