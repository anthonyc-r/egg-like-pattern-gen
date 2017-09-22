#!/bin/bash
export CLASSPATH=".:/usr/local/share/java/commons-math3-3.6.jar:/usr/local/share/java/gluegen-rt.jar:/usr/local/share/java/jogl-all.jar:/usr/local/java/jogl-all-natives-linux-amd64.jar:/usr/local/java/gluegen-rt-natives-linux-amd64.jar"

rm generation/*.class
rm simulation/*.class
javac generation/*.java
javac simulation/*.java
java simulation.Main
