#!/bin/bash

export LC_ALL=C

set -e
set -o pipefail

trap 'echo ERROR; read a' ERR

cd "$(dirname "$0")"

SRC="mp3/"
DEST="../android/assets/mp3-challenges/"

rsync -a --delete-after --human-readable --progress --verbose "$SRC" "$DEST"

exit 0
