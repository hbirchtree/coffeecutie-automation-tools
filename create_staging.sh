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

echo " * Staging directory prepared, to make release, do:
   \$ gh release upload <tag name> $STAGING_DIR/*"
