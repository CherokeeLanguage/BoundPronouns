#!/bin/bash

#DejaVu Book: /usr/share/fonts/truetype/ttf-dejavu/DejaVuSans.ttf
#FreeSerif Medium: /usr/share/fonts/truetype/freefont/FreeSerif.ttf
#unifont: /usr/share/fonts/truetype/unifont/unifont.ttf


DEST=img2
DESTB=img.small

if [ -d "${DEST}" ]; then rm -rf "${DEST}"; fi
if [ ! -d "${DEST}" ]; then mkdir "${DEST}"; fi

F1="/usr/share/fonts/truetype/ttf-dejavu/DejaVuSans.ttf"
#F1="/usr/share/fonts/truetype/ttf-dejavu/FreeSansBold.ttf"

for glyph in ║ - _ = + +0 -0 ✘ ✓ ✔ ✕ ✖ ✗ ✘ ♫ ⚀ ⚁ ⚂ ⚃ ⚄ ⚅ ☐ ☑ ☒ ☓ ☠ ☹ ☺ 0 +1 +2 +3 +4 +5 -1 -2 -3 -4 -5; do
#for glyph in ☐; do
ix=0
for color in white; do # firebrick blue red green purple orange yellow black brown gold2 gold3 cornsilk4 crimson; do
file="_${glyph}_${color}".png
gm convert \
-background none \
	-depth 24 \
    -fill $color \
    -stroke none \
    -strokewidth 0 \
    -font "$F1" \
	-filter Cubic \
    -size 512x512 \
    label:"$glyph" \
    -trim \
    "${DEST}"/"$file"    
gm mogrify -background none -gravity center -filter Cubic -resize 256x256 -extent 256x256 "${DEST}"/"$file"
cp "${DEST}"/"$file" "${DEST}"/"small-$file"
gm mogrify -background none -gravity center -filter Cubic -resize 32x32 -extent 32x32 "${DEST}"/"small-$file"
ix=$(($ix+1))
done
done
