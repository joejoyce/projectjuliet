#!/bin/sh

leases=/var/lib/dhcp/dhclient.eth0.leases
dhcpServer=`grep '^\s*option\s\s*dhcp-server-identifier\s\s*' $leases | sed 's/#.*//g' | tail -n 1 | awk '{print $3}' | sed 's/;.*//g'`
echo "DHCP server address is $dhcpServer."

if [ ! -d '/juliet' ]; then
    mkdir '/juliet'
fi
while :; do

if mountpoint -q /juliet || mount -t cifs //$dhcpServer/juliet /juliet -o guest,sec=none; then
    echo 'Mounted //'$dhcpServer'/juliet at /juliet'
else
    echo 'Failed to mount //'$dhcpServer'/juliet'
    sleep 10;
    continue;
fi

if cp /juliet/cluster.jar /juliet/mysql-connector-java-5.1.29-bin.jar / ; then
    sleep 20;
    umount /juliet;
    java -cp /mysql-connector-java-5.1.29-bin.jar:/cluster.jar:/ uk.ac.cam.cl.juliet.slave.listening.Client $dhcpServer;
else
    echo 'Failed to copy JARs'
    sleep 20;
fi
done
