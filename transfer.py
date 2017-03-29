#!/usr/bin/env python
import sys
import struct
from scapy.all import *
from struct import pack
from struct import unpack

to_hex = lambda x:" ".join([hex(ord(c)) for c in x])

def transfer(label):
    if label == 0x1:
        print ":Skype"

def handle_pkt(pkt):
    try :
        label = (str(pkt))[0:1]
        print to_hex(label);
        reason = (str(pkt))[1:2]
        packet = (str(pkt))[2:]
        original_pkt = Ether(packet)
        counter = 0
        while True:
            layer = original_pkt.getlayer(counter)

            if layer == None : break
            elif layer.name == 'IP' :
                src_ip = original_pkt['IP'].fields['src']
                dst_ip = original_pkt['IP'].fields['dst']
            elif layer.name == 'TCP' :
                src_port = original_pkt['TCP'].fields['sport']
                dst_port = original_pkt['TCP'].fields['dport']
            elif layer.name == 'UDP' :
                src_port = original_pkt['UDP'].fields['sport']
                dst_port = original_pkt['UDP'].fields['dport']
            
            counter += 1


        print "%s:%s <-> %s:%s " % ( src_ip, src_port, dst_ip, dst_port)
        transfer(int(to_hex(label),16))
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

