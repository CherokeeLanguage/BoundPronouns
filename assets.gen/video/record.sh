#!/bin/bash

cd "$(dirname "$0")"

length="00:01:00"

w=1280
h=720

offsetw=6
offseth=24

rw=$((2500 + $offsetw))
rh=$((0 + $offseth))

OUT=recording-${w}-${h}.mp4

cd "$(dirname "$0")"

export JAR="$(ls ../../desktop/build/libs/BoundPronouns*.jar|sort -r|head -n 1)"

echo "${JAR}"

#pactl list sources|less
AUDIO1="alsa_input.pci-0000_00_1b.0.analog-stereo"
AUDIO2="alsa_output.pci-0000_00_1b.0.analog-stereo.monitor"
RESYNC1="aresample=async=1:min_hard_comp=0.100000:first_pts=0"
RESYNC2="aresample=async=10000"
pacmd set-default-source "${AUDIO2}"

java -jar "${JAR}" &

count=10
nextwindow=0
while [ "$(wmctrl -l | grep 'N/A' | grep BoundPronouns$ | cut -f 1 -d ' ')"x = x ]; do
	sleep 1;
	count=$(($count-1))
	if [ "$count" = "0" ]; then
		echo "FATAL."
		read a
		exit 0
	fi
done

id="$(wmctrl -l | grep 'N/A' | grep BoundPronouns$ | cut -f 1 -d ' ' | head -n 1)"
id="BoundPronouns"
wmctrl -F -r "$id" -e 0,2500,0,${w},${h}

#https://launchpad.net/~jon-severinsson/+archive/ffmpeg

rm "${OUT}" || true

ffmpeg -strict -2 -f alsa -ac 2 -i pulse -f x11grab -draw_mouse 0 -show_region 1 -strict -2 -acodec aac -r 30 -s ${w}x${h} -i "${DISPLAY}+${rw},${rh}" -strict -2 -vcodec libx264 -preset ultrafast -threads 0 -af "$RESYNC2" -t "$length" "$OUT"

wmctrl -F -c "$id"

echo "DONE"
read a


