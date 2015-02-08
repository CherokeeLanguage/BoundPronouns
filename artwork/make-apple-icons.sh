#!/bin/bash

export FILTER=Lanczos

set -e

cd "$(dirname "$0")"

mkdir applecons || true 2> /dev/null

gm convert -filter ${FILTER} icon.png -resize 72x72 applecons/Icon-72.png
gm convert -filter ${FILTER} icon.png -resize 144x144 applecons/Icon-72@2x.png
gm convert -filter ${FILTER} icon.png -resize 57x57 applecons/Icon.png
gm convert -filter ${FILTER} icon.png -resize 114x114 applecons/Icon@2x.png
