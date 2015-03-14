#!/bin/bash

cd "$(dirname "$0")"

w=960
h=640

OUT=recording-${w}-${h}.mkv

cd "$(dirname "$0")"

export JAR="$(ls ../../desktop/build/libs/BoundPronouns*.jar|sort -r|head -n 1)"

echo "${JAR}"

java -jar "${JAR}" &

sleep 1

count=10
nextwindow=0
while [ "$(wmctrl -l | grep 'N/A' | grep BoundPronouns | cut -f 1 -d ' ')"x = x ]; do
	sleep 1;
	count=$(($count-1))
	if [ "$count" = "0" ]; then
		echo "FATAL."
		read a
		exit 0
	fi
done

length="00:00:30"

id="$(wmctrl -l | grep 'N/A' | grep BoundPronouns | cut -f 1 -d ' ' | head -n 1)"
wmctrl -i -r "$id" -e 0,2560,0,${w},${h}

echo "ID: ${id}"
echo wmctrl -i -r "$id" -e 0,2560,0,${w},${h}

#pactl list sources|less
AUDIO1="alsa_input.pci-0000_00_1b.0.analog-stereo"
AUDIO2="alsa_output.pci-0000_00_1b.0.analog-stereo.monitor"
RESYNC1="aresample=async=1:min_hard_comp=0.100000:first_pts=0"
RESYNC2="aresample=async=10000"
pacmd set-default-source "${AUDIO2}"

#https://launchpad.net/~jon-severinsson/+archive/ffmpeg

ffmpeg -f alsa -ac 2 -i pulse -f x11grab -acodec pcm_s16le -r 30 -s ${w}x${h} -i "${DISPLAY}+2560x0" -vcodec libx264 -preset ultrafast -threads 0 -af "$RESYNC2" -t "$length" "$OUT"

wmctrl -i -c "$id"
id="$(wmctrl -l | grep BoundPronouns | cut -f 1 -d ' ' | head -n 1)"
if [ "$id"x != x ]; then wmctrl -i -c "$id"; fi

echo "DONE"
read a


