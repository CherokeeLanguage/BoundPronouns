#!/bin/bash

set -e
set -o pipefail

trap 'echo ERROR' ERR

cd "$(dirname "$0")"

#Rebuild conjugations table for use by espeak-ng phase
./gradlew clean || exit 1
./gradlew core:build || exit 1 

#Always auto commit the conjugation table data via git
git add android/assets/deck.json
git add android/assets/espeak.tsv
git add android/assets/review-sheet.tsv
git commit -m "Updated master deck file." || true #ignore if no changes to commit.


if ! git diff-index --quiet HEAD --; then
    git status
    echo
    echo "PENDING CHANGES NOT COMMITTED - ABORTING [post deck rebuild]"
    echo
    exit -1
fi

#Always rebuild and resync audio.
bash ./espeak-ng/build-mp3s.sh
bash ./espeak-ng/sync-mp3s.sh
#Always re-add audio files if any changed.
git add android/assets/mp3-challenges
git commit -m "Updated audio files." || true #ignore if no changes to commit.
git gc

if ! git diff-index --quiet HEAD --; then
    git status
    echo
    echo "PENDING CHANGES NOT COMMITTED - ABORTING [post audio rebuild]"
    echo
    exit -1
fi

#Ensure the project can be full built before doing anything else.
./gradlew clean || exit 1
./gradlew core:build || exit 1
./gradlew desktop:dist || exit 1
./gradlew android:assembleRelease || exit 1
./gradlew clean || exit 1

#Make sure we have an up-to-date git log in the text folder as "git-changelog.txt"
git log --simplify-merges --pretty=format:"%ad [%h]:%d %s" --abbrev-commit --date=short > android/assets/text/git-changelog.txt
git add android/assets/text/git-changelog.txt
git commit android/assets/text/git-changelog.txt -m "Update git changelog." || true

version=$(head -n1 version)
version=$(($version + 1 ))
xversion="${version:0:${#version}-2}.${version: -2}"

echo "==================================="
echo "BUILD RELEASE: $xversion ($version)"
echo "-----------------------------------"

sed -i "s/version = '.*'/version = '$xversion'/g" build.gradle
sed -i "s/versionCode=\".*\"/versionCode=\"$version\"/g" android/AndroidManifest.xml
sed -i "s/versionName=\".*\"/versionName=\"$xversion\"/g" android/AndroidManifest.xml
sed -i "s/app.version=.*$/app.version=$xversion/g" ios/robovm.properties

echo "$version" > version

git add version
git commit -a -m "Bump version for release build." || true
git tag "${xversion}" || true

#Build the newly tagged version.
./gradlew clean
./gradlew core:build
./gradlew desktop:dist
./gradlew android:assembleRelease

exit 0
