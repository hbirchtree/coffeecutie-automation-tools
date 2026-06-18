.PHONY:

eglinfo.ext: eglinfo.cpp .PHONY
	compiler-arm-linux-gnueabi/arm-maemo-linux-gnueabi/bin/arm-maemo-linux-gnueabi-g++ eglinfo.cpp -Wl,--allow-shlib-undefined -lEGL -lX11 -lXau -lXdmcp -lGLESv2 -o $@

# -Wl,--allow-shlib-undefined 
