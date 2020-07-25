#!/bin/bash

docker run -it --rm --init --gpus=all \
	--ipc=host --volume="$PWD:/workspace" \
	-e NVIDIA_VISIBLE_DEVICES=0 -p 2150:22 \
	--device /dev/snd voice-base

echo "=== DONE"
exit 0

nvidia-smi
cd /workspace/Real-Time-Voice-Cloning
python demo_cli.py
exit