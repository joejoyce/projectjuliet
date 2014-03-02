Resilient and Rapid Raspberries
=============
Team Juliet | teamjuliet.co.uk
-------------------------------


Run master PC components:

sudo java -Xms1024M -Xmx1024M -classpath .:../mysql-connector-java-5.1.29-bin.jar:uk/ac/cam/cl/juliet/master/ uk.ac.cam.cl.juliet.master.ClusterServer /home/juliet_data/20111219-ARCA_XDP_IBF_1.dat /home/juliet_data/20111219-ARCA_XDP_IBF_2.dat /home/juliet_data/20111219-ARCA_XDP_IBF_3.dat /home/juliet_data/20111219-ARCA_XDP_IBF_4.dat 0

Run client:

java -classpath ../mysql-connector-java-5.1.29-bin.jar:.:uk/ac/cam/cl/juliet/slave/listening/ uk.ac.cam.cl.juliet.slave.listening.Client 127.0.0.1

Build:

./scripts/build.sh
