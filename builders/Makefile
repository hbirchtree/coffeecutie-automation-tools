.PHONY: FORCE

help:
	@echo "Build commands:\n\
*.build          - build an image\n\
*.build-cached   - build an image, uses cache\n\
*.push           - push an image to DockerHub\n\
*.update         - build and push image\n\
\n\
cmake.refresh    - refresh CMake image, uncached\n\
cmake-images     - build all CMake-dependent images, also refreshes CMake\n\
\n\
devkit.build.*   - build a devkit image under the devkit/ dir\n\
devkit.push.*    - push a devkit image\n\
devkit.update.*  - build and push\n\
\n\
android.tag      - set the tagged release for Android\n\
android.r*       - build a certain Android NDK version\n\
android-versions - build r15c and r20b\n\
\n\
all-build, all-push\n\
"

all: all-build all-push

FORCE:
	@true

cmake.refresh: FORCE
	docker build --no-cache -t hbirch/ubuntu-cmake cmake-latest

%.build: %/Dockerfile FORCE
	# Avoid cache to refresh images fully
	docker build --no-cache -t hbirch/coffeecutie:$* $*

%.build-cached: %/Dockerfile FORCE
	docker build -t hbirch/coffeecutie:$* $*

%.push: FORCE
	docker push hbirch/coffeecutie:$*

%.update: %.build %.push

cmake-images: cmake.refresh ubuntu.build emscripten.build fedora.build raspberry.build doc-generator.build mingw64.build beaglebone.build

devkit.build.%: devkit/Dockerfile.% devkit/config.%.sh
	docker build --no-cache -t hbirch/devkit:$* devkit -f devkit/Dockerfile.$*

devkit.push.%:
	docker push hbirch/devkit:$*

devkit.update.%: devkit.build.% devkit.push.%

android.r%: android/Dockerfile
	docker build --no-cache --build-arg ANDK_VER=$* -t hbirch/android:r$* android

android.tag:
	docker tag hbirch/android:r15c hbirch/coffeecutie:android

android-versions: android.r15c android.r20b android.tag

all-build: cmake-images android-versions devkit.build.ppc

all-push: ubuntu.push emscripten.push fedora.push raspberry.push doc-generator.push android.push devkit.push.ppc

