%.build: %/Dockerfile
	# Avoid cache to refresh images fully
	docker build --no-cache -t hbirch/coffeecutie:$* $*

%.push:
	docker push hbirch/coffeecutie:$*

%.update: %.build %.push

cmake-images: cmake-latest.build ubuntu.build emscripten.build fedora.build raspberry.build doc-generator.build mingw64.build

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

all-build: cmake-images android-versions

all-push: ubuntu.push emscripten.push fedora.push raspberry.push doc-generator.push android.push

all: all-build all-push
