#!/bin/bash

export FILTER=Lanczos

set -e

cd "$(dirname "$0")"

mkdir amazon 2> /dev/null || true

x=0
for IMG in nook/*.png; do
	if [ ! -f "${IMG}" ]; then continue; fi
	x=$(($x+1))
	DIMG="$(printf "%03d" $x)".png
	gm convert -filter ${FILTER} "${IMG}" -crop 1920x1080+0+58 amazon/"${DIMG}"
done
