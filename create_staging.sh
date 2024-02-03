#!/bin/bash

TARGETS=$(jq -r .sources[].name meta.json)
STAGING_DIR=$(dirname $0)/staging

mkdir -p $STAGING_DIR

echo " * Linking in targets:"
for TARGET in $TARGETS; do
    FILES=$(jq -r '.sources[] | select(.name=="'$TARGET'") | .mapping | keys[]' meta.json)
    echo "  - Adding $(jq -r '.sources[] | select(.name=="'$TARGET'") | .toolchain' meta.json)"
    for FILE in $FILES; do
        DESTINATION=$(jq -r '.sources[] | select(.name=="'$TARGET'") | .mapping."'$FILE'"' meta.json)
        ln $FILE $STAGING_DIR/$DESTINATION
    done
done

echo "  - Generating registry file"
jq .outputs meta.json > $STAGING_DIR/registry.json

echo "  - Generating release notes"
echo "# Release <tag name>" > $STAGING_DIR/release-notes.md
for TARGET in $TARGETS; do
    ARCH=$(jq -r '.sources[] | select(.name=="'$TARGET'") | .architecture' meta.json)
    PLATFORM=$(jq -r '.sources[] | select(.name=="'$TARGET'") | .platform' meta.json)
    MANIFEST=$(cat $(jq -r '.sources[] | select(.name=="'$TARGET'") | .manifest' meta.json))
    COMPILER_VERSION=$(echo "$MANIFEST" | grep CT_GCC_VERSION | cut -d'"' -f2)
    GLIBC_VERSION=$(echo "$MANIFEST" | grep 'CT_GLIBC_VERSION\|CT_NEWLIB_VERSION' | cut -d'"' -f2)
    BINUTILS_VERSION=$(echo "$MANIFEST" | grep CT_BINUTILS_VERSION | cut -d'"' -f2)
    echo " - ${PLATFORM} ${ARCH}
   - GCC version: ${COMPILER_VERSION}
   - libc version: ${GLIBC_VERSION}
   - binutils version: ${BINUTILS_VERSION}" >> $STAGING_DIR/release-notes.md
done

echo " * Staging directory prepared, to make release, do:
   \$ gh release upload <tag name> $STAGING_DIR/*"
