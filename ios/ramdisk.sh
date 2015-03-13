#!/bin/sh



SIZE=1024 ; diskutil erasevolume HFS+ 'RoboVM RAM Disk' `hdiutil attach -nomount ram://$((SIZE * 2048))`
