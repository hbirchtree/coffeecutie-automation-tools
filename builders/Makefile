.PHONY: FORCE

all: all-build all-push

FORCE:
	true

cmake.refresh: FORCE
	docker build --no-cache -t hbirch/ubuntu-cmake cmake-latest

%.build: %/Dockerfile FORCE
	# Avoid cache to refresh images fully
	docker build --no-cache -t hbirch/coffeecutie:$* $*

%.push: FORCE
	docker push hbirch/coffeecutie:$*

%.update: %.build %.push

cmake-images: cmake.refresh ubuntu.build emscripten.build fedora.build raspberry.build doc-generator.build mingw64.build

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

