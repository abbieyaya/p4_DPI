#!/usr/bin/env python
import sys
import struct
from scapy.all import *
from struct import pack
from struct import unpack

to_hex = lambda x:" ".join([hex(ord(c)) for c in x])

table=dict()
def match(src_ip,src_port,dst_ip,dst_port,m_label,s_label):
    key = frozenset({src_ip+":"+str(src_port), dst_ip+":"+str(dst_port)})
    value = set({m_label,s_label})
    if key not in table :
        table.update({key:value})
        print "%s:%s <-> %s:%s , %s.%s" % ( src_ip, src_port, dst_ip, dst_port, m_label, s_label)

def master_label(label):
    if label == 1:
        return "Skype"
    if label == 2:
        return "DNS"
    if label == 3:
        return "QUIC"
    if label == 4:
        return "Whatsapp"

def sub_label(label):
    if label == 1:
        return "yahoo"
    if label == 2:
        return "google"
    if label == 3:
        return "youtube"

def handle_pkt(pkt):
    try :
        # get label name
        m_label = master_label(int(to_hex((str(pkt))[0:1]), 16))
        s_label = sub_label(int(to_hex((str(pkt))[1:2]), 16))
      
        packet = Ether((str(pkt))[2:])
        # parse five tuple
        ether_packet = Ether((str(pkt))[2:16])
        #print to_hex(str(pkt)[14:15]) == '0x86' and to_hex(str(pkt)[15:16]) == '0xdd'
        if( to_hex(str(pkt)[16:17]) == '0x0' and to_hex(str(pkt)[17:18]) == '0x0' ) : 
            #print "fk" 
            sys.stdout.flush()
            ip_packet = IP((str(pkt))[18:])
            if( to_hex(str(pkt)[14:15]) == '0x86' and to_hex(str(pkt)[15:16]) == '0xdd' ) : ip_packet = IPv6((str(pkt))[18:])
            else : ip_packet = IP((str(pkt))[18:])
        else : 
            #print "good" 
            sys.stdout.flush()
            ip_packet = IP((str(pkt))[16:])
            if( to_hex(str(pkt)[14:15]) == '0x86' and to_hex(str(pkt)[15:16]) == '0xdd' ) : ip_packet = IPv6((str(pkt))[16:])
            else : ip_packet = IP((str(pkt))[16:])
    except:
        print "Parse Wrong"

    
    #packet.show()
    #ether_packet.show()
    #ip_packet.show()
    packet = ip_packet 
    try :
        counter = 0
        while True:
            layer = ip_packet.getlayer(counter)

            #print "layer = ", layer.name
            if layer.name == 'IP' :
                src_ip = packet['IP'].fields['src']
                dst_ip = packet['IP'].fields['dst']
            elif layer.name == 'IPv6':
                src_ip = packet['IPv6'].fields['src']
                dst_ip = packet['IPv6'].fields['dst']
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
    except:
        print "Get five tuple Wrong"


    try :
        match(src_ip,src_port,dst_ip,dst_port,m_label,s_label)
    except:
        print "Print Wrong"
        

def main():
    from sys import argv
    if len(argv) < 2:
        print "Usage transfer.py [host number/file]"
        return

    #if argv[1].find(".pcap") == -1 : target =  "h%s-eth1" % (argv[1])
    if argv[1].find(".pcap") == -1 : target =  argv[1]
    else : target = "h%s-eth1" % (argv[1])
    
    print "Listen on %s to transfer label of the packet" % target
    if target.find(".pcap") == -1 : sniff(iface=target, prn=handle_pkt )
    else : sniff(offline=target, prn=handle_pkt)

if __name__ == '__main__':
    main()

