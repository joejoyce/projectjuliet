#!/bin/sh
cd /home/juliet/projectjuliet/src
find -name "*.java" > sources.txt 
javac @sources.txt 
rm sources.txt

find uk/ac/cam/cl/juliet/slave/ -name "*.class" > classes.txt
find uk/ac/cam/cl/juliet/common/ -name "*.class" >> classes.txt
jar cfe ../../share/cluster.jar uk.ac.cam.cl.juliet.slave.listening.Client @classes.txt
rm classes.txt

