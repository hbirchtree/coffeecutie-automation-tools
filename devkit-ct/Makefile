dkppc-shell:
	docker run \
		--rm -ti \
		-v $(PWD):/output \
		--user 1000 \
		--workdir /opt/devkitpro \
		devkitpro/devkitppc:20260503

dkppc-cube-steal:
	rm -r $(PWD)/powerpc-eabi-cube
	docker run \
		--rm -ti \
		-v $(PWD):/output \
		--user 1000 \
		devkitpro/devkitppc:20260503 \
		\
		cp -r /opt/devkitpro/devkitPPC /output/powerpc-eabi-cube
	docker run \
		--rm -ti \
		-v $(PWD):/output \
		--user 1000 \
		devkitpro/devkitppc:20260503 \
		\
		cp -r /opt/devkitpro/libogc /output/powerpc-eabi-cube/powerpc-eabi
