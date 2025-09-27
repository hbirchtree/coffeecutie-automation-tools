# Contains recipes for building toolchains

## Creating a release

In order to create a release, one must build all applicable targets using the root `Makefile`, ensure compilers and sysroots are built. Commonly done by:

    make desktop-x86_64-buildroot-linux-gnu.build

If any errors happen during building a toolchain, that must be fixed. It should be possible to build the compiler and sysroot from a clean slate.

Afterwards, one must generate the list of manifests and their files:

    make meta.json

This generates a manifest of toolchains to include in the staging directory which will be uploaded to a release:

    ./create_staging.sh

At this point everything is ready for release:

    gh release create <tag name>
    gh release upload <tag name> staging/*
