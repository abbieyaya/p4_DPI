#!/bin/bash
# Program:
#       Use loop to run ndpi

while [ 1 ]
do
    sudo tcpdump -i eth0 -w live.pcap -c 100
    sudo tcpreplay -i s1-eth1 live.pcap
done

