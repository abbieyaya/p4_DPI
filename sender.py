#!/usr/bin/python
from scapy.all import *
from time import sleep
from random import randint

def main():

    count = 1
    while(1):
        dst = randint(2, 3)
        p = "\x00\x01\x00" + chr(dst) + "Hello from h1 (%d)" % (count, )
        count += 1
        sendp(p, iface = "h1-eth1")
        print "Send 1 Packet to h%d" % (dst, )
        sleep(1)

if __name__ == '__main__':
    main()

