#!/bin/bash

set -e
set -o pipefail

trap 'echo ERROR' ERR

cd "$(dirname "$0")"

version=$(head -n1 version)
xversion="${version:0:${#version}-2}.${version: -2}"
git add .
git commit -a -m "autocommit  for tagging."
git tag "$xversion" || true
git push --tags

version=$(($version + 1 ))
xversion="${version:0:${#version}-2}.${version: -2}"

xversion="${version:0:${#version}-2}.${version: -2}"

echo "$xversion ($version)"

sed -i "s/version = '.*'/version = '$xversion'/g" build.gradle
sed -i "s/versionCode=\".*\"/versionCode=\"$version\"/g" android/AndroidManifest.xml
sed -i "s/versionName=\".*\"/versionName=\"$xversion\"/g" android/AndroidManifest.xml
sed -i "s/app.version=.*$/app.version=$xversion/g" ios/robovm.properties

echo "$version" > version

git add .
git commit -a -m "Start of next development cycle."
git tag "${xversion}-PRE" || true
git push --tags

exit 0
