#!/usr/bin/python
from scapy.all import *
from time import sleep
from random import randint

def main():

    count = 1
    while(1):
        target = randint(2, 3)
        p = Ether(dst="00:00:00:00:00:0%d" % target) / IP(dst="10.0.0.%d" % target) / TCP() / "aaaaaaaaaaaaaaaaaaa"
        count += 1
        sendp(p, iface = "h1-eth1")
        print "Send 1 Packet to h%d" % (target )
        sleep(1)

if __name__ == '__main__':
    main()

