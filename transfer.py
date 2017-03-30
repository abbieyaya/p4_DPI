#!/usr/bin/env python
import sys
import struct
from scapy.all import *
from struct import pack
from struct import unpack

to_hex = lambda x:" ".join([hex(ord(c)) for c in x])

def transfer(label):
    if label == 1:
        return "Skype"

def handle_pkt(pkt):
    try :
        # get label name
        label = transfer(int(to_hex((str(pkt))[0:1]), 16))

        # get some info
        reason = (str(pkt))[1:2]

        # parse five tuple
        packet = Ether((str(pkt))[2:])

        counter = 0
        while True:
            layer = packet.getlayer(counter)

            if layer.name == 'IP' :
                src_ip = packet['IP'].fields['src']
                dst_ip = packet['IP'].fields['dst']
            elif layer.name == 'TCP' :
                src_port = packet['TCP'].fields['sport']
                dst_port = packet['TCP'].fields['dport']
                break ;
            elif layer.name == 'UDP' :
                src_port = packet['UDP'].fields['sport']
                dst_port = packet['UDP'].fields['dport']
                break ;
            elif layer == None : break

            counter += 1

        # show results
        print "%s:%s <-> %s:%s , %s" % ( src_ip, src_port, dst_ip, dst_port, label)
        sys.stdout.flush()
        
    except:
        print "Something wrong..."

def main():
    from sys import argv
    if len(argv) < 2:
        print "Usage transfer.py [host number/file]"
        return

    if argv[1].find(".pcap") == -1 : target =  "h%s-eth1" % (argv[1])
    else : target = argv[1]
    
    print "Listen on %s to transfer label of the packet" % target
    if target.find(".pcap") == -1 : sniff(iface=target, prn=handle_pkt )
    else : sniff(offline=target, prn=handle_pkt)

if __name__ == '__main__':
    main()

