#!/bin/sh

leases=/var/lib/dhcp/dhclient.eth0.leases
dhcpServer=`grep '^\s*option\s\s*dhcp-server-identifier\s\s*' $leases | sed 's/#.*//g' | tail -n 1 | awk '{print $3}' | sed 's/;.*//g'`
echo "DHCP server address is $dhcpServer."

if [ ! -d '/juliet' ]; then
    mkdir '/juliet'
fi

if mountpoint -q /juliet || mount -t cifs //$dhcpServer/juliet /juliet -o guest,sec=none; then
    echo 'Mounted //'$dhcpServer'/juliet at /juliet'
else
    echo 'Failed to mount //'$dhcpServer'/juliet'
    exit 1
fi

if cp /juliet/cluster.jar /juliet/mysql-connector-java-5.1.29-bin.jar / ; then
    java -cp /cluster.jar:/mysql-connector-java-5.1.29-bin.jar -jar /cluster.jar $dhcpServer &
else
    echo 'Failed to copy JARs'
    exit 1
fi
