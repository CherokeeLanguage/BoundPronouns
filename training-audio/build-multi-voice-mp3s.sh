#!/bin/bash

export LC_ALL=C

set -e
set -o pipefail

trap 'echo ERROR; read a' ERR

vol=100

cd "$(dirname "$0")"

ff="tmp-ffmpeg.sh"
cp /dev/null "$ff"

function rebuildEspeak {
    z="$(pwd)"
    cd ~/git/espeak-ng
    make
    make install
    cd "$z"
}

function dospeak_chr {
    local txt="${1}"
    local filename="${2}"
    local variant="${3}"

    local mp3="$filename".mp3
    local wav="$filename".wav

    #echo "echo $txt" >> "$ff"
    echo "${HOME}/espeak-ng/bin/espeak-ng -a $vol -v chr+${variant} -w \"$wav\" \"$txt\"" >> "$ff"
    echo "normalize-audio -q \"$wav\"" >> "$ff"
    #echo "ffmpeg -y -i \"$wav\" -codec:a libmp3lame -qscale:a 3 \"$mp3\" > /dev/null 2>&1" >> "$ff"
    echo "ffmpeg -y -i \"$wav\" -codec:a libmp3lame -qscale:a 6 \"$mp3\" > /dev/null 2>&1" >> "$ff"
    echo "rm \"$wav\"" >> "$ff"
    echo >> "$ff"
}

rebuildEspeak

file="../android/assets/espeak.tsv"

VOICES=""
for VOICE in "${HOME}"/espeak-ng/share/espeak-ng-data/voices/!v/*; do
    VOICES="$VOICES $(basename "$VOICE")"
done
VOICES="default $VOICES"

for voice in $VOICES; do
    mp3dir="./mp3/$voice"

    if [ -d "$mp3dir" ]; then
        for mp3 in "$mp3dir"/*.mp3; do
            if [ -f "$mp3" ]; then rm "$mp3"; fi
        done
    else
        mkdir -p "$mp3dir"
    fi
done

for voice in $VOICES; do
    echo " - generating ffmpeg script commands for $voice"
    mp3dir="./mp3/$voice"
    (cat "$file" | head -n 30 || true) | while read line; do
        syl="$(echo "$line" | cut -f 1)"
        chr="$(echo "$line" | cut -f 2)"
        filename="$(echo "$line" | cut -f 3)"
        dospeak_chr "$chr" "$mp3dir/$filename" "$voice"
    done
done

echo " - generating wavs, normalizing, and converting to mp3s"
bash "$ff"
rm "$ff"

exit 0
