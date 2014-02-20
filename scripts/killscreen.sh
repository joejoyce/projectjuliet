#!/bin/sh
screen -ls | grep -o '[0-9]*\.'|  cut -d. -f1 | awk '{print $1}' | xargs kill

