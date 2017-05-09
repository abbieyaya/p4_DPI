#!/usr/bin/env python
import sys
import struct
from scapy.all import *
from struct import pack
from struct import unpack

to_hex = lambda x:" ".join([hex(ord(c)) for c in x])

table=dict()
def match(src_ip,src_port,dst_ip,dst_port,m_label,s_label, m_label_result, s_label_result):
    key = frozenset({src_ip+":"+str(src_port), dst_ip+":"+str(dst_port)})
    value = set({m_label,s_label})
    if key not in table :
        table.update({key:value})
        if m_label == "" : 
            print "%s:%s <-> %s:%s , %s (%s)" % ( src_ip, src_port, dst_ip, dst_port, s_label, s_label_result)
        elif s_label == "" : 
            print "%s:%s <-> %s:%s , %s (%s)" % ( src_ip, src_port, dst_ip, dst_port, m_label, m_label_result)
        else : 
            print "%s:%s <-> %s:%s , %s.%s (%s.%s)" % ( src_ip, src_port, dst_ip, dst_port, m_label, s_label, m_label_result, s_label_result)

def master_label(label):
    #print label
    if label == 0: return ""
    if label == 1: return "Skype"
    if label == 2: return "DNS"
    if label == 3: return "QUIC"
    if label == 4: return "Whatsapp"
    if label == 5: return "BitTorrent"
    if label == 6: return "Teamviewer"
    if label == 7: return "Youku"
    if label == 8: return "FTP_CONTROL"
    if label == 9: return "FTP_DATA"
    if label == 10: return "POP3"
    if label == 11: return "POPS"
    if label == 12: return "SMTP"
    if label == 13: return "SMTPS"
    if label == 14: return "IMAP"
    if label == 15: return "IMAPS"
    if label == 16: return "HEP"
    if label == 17: return "NetBIOS"
    if label == 18: return "NFS"
    if label == 19: return "BGP"
    if label == 20: return "XDMCP"
    if label == 21: return "SMB"
    if label == 22: return "Syslog"
    if label == 23: return "PostgreSQL"
    if label == 24: return "MySQL"
    if label == 25: return "VMware"
    if label == 26: return "BitTorrent"
    if label == 27: return "RTSP"
    if label == 28: return "IRC"
    if label == 29: return "Telnet"
    if label == 30: return "IPsec"
    if label == 31: return "OSPF"
    if label == 32: return "RDP"
    if label == 33: return "VNC"
    if label == 34: return "SSH"
    if label == 35: return "IAX"
    if label == 36: return "AFP"
    if label == 37: return "Kerberos"
    if label == 38: return "SIP"
    if label == 39: return "LDAP"
    if label == 40: return "MsSQL-TDS"
    if label == 41: return "DCE_RPC"
    if label == 42: return "HTTP_Proxy"
    if label == 43: return "LotusNotes"
    if label == 44: return "Citrix"
    if label == 45: return "Radius"
    if label == 46: return "SAP"
    if label == 47: return "UPnP"
    if label == 48: return "LLMNR"
    if label == 49: return "RemoteScan"
    if label == 50: return "OpenVPN"
    if label == 51: return "H323"
    if label == 52: return "CiscoVPN"
    if label == 53: return "CiscoSkinny"
    if label == 54: return "RSYNC"
    if label == 55: return "Oracle"
    if label == 56: return "Whois-DAS"
    if label == 57: return "SOCKS"
    if label == 58: return "RTMP"
    if label == 59: return "Redis"
    if label == 60: return "Starcraft"
    if label == 61: return "MQTT"
    if label == 62: return "Git"
    if label == 63: return "MDNS"
    if label == 64: return "NTP"
    if label == 65: return "SNMP"
    if label == 66: return "DHCP"
    if label == 67: return "Teredo"
    if label == 68: return "Ayiya"
    if label == 69: return "STUN"
    if label == 70: return "NetFlow"
    if label == 71: return "sFlow"
    if label == 72: return "GTP"
    if label == 73: return "Dropbox"
    if label == 74: return "EAQ"
    if label == 75: return "Collectd"
    if label == 76: return "TFTP"
    if label == 77: return "Megaco"
    if label == 78: return "VHUA"
    if label == 79: return "UBNTAC2"
    if label == 80: return "Viber"
    if label == 81: return "COAP"
    if label == 82: return "BJNP"
    return ""

def sub_label(label):
    #print label
    if label == 0: return ""
    if label == 1: return "Skype"
    if label == 2: return "Yahoo"
    if label == 3: return "Wikipedia"
    if label == 4: return "Whatsapp"
    if label == 5: return "BitTorrent"
    if label == 6: return "GoogleMaps"
    if label == 7: return "Gmail"
    if label == 8: return "Facebook"
    if label == 9: return "Twitter"
    if label == 10: return "Wechat"
    if label == 11: return "Netflex"
    if label == 12: return "Apple"
    if label == 13: return "Google"
    if label == 14: return "Dropbox"
    if label == 15: return "Twitch"
    if label == 16: return "Github"
    if label == 17: return "Steam"
    if label == 18: return "PPStream"
    if label == 19: return "Instagram"
    if label == 20: return "CNN"
    if label == 21: return "Youtube"
    return ""

def detect_or_guess(label):
    if label == 1:
        return "detect"
    if label == 2:
        return "guess"

def handle_pkt(pkt):
    try :
        # get label name
        m_label = master_label(int(to_hex((str(pkt))[0:1]), 16))
        s_label = sub_label(int(to_hex((str(pkt))[1:2]), 16))
        m_label_result = detect_or_guess(int(to_hex((str(pkt))[2:3]), 16))
        s_label_result = detect_or_guess(int(to_hex((str(pkt))[3:4]), 16))
      
        packet = Ether((str(pkt))[4:])
        # parse five tuple
        ether_packet = Ether((str(pkt))[4:18])
        #print to_hex(str(pkt)[14:15]) == '0x86' and to_hex(str(pkt)[15:16]) == '0xdd'
        if( to_hex(str(pkt)[18:19]) == '0x0' and to_hex(str(pkt)[19:20]) == '0x0' ) : 
            #print "fk" 
            #sys.stdout.flush()
            #ip_packet = IP((str(pkt))[20:])
            if( to_hex(str(pkt)[16:17]) == '0x86' and to_hex(str(pkt)[17:18]) == '0xdd' ) : ip_packet = IPv6((str(pkt))[20:])
            else : ip_packet = IP((str(pkt))[20:])
        else : 
            #print "good" 
            #sys.stdout.flush()
            #ip_packet = IP((str(pkt))[18:])
            if( to_hex(str(pkt)[16:17]) == '0x86' and to_hex(str(pkt)[17:18]) == '0xdd' ) : ip_packet = IPv6((str(pkt))[18:])
            else : ip_packet = IP((str(pkt))[18:])
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
        match(src_ip,src_port,dst_ip,dst_port,m_label,s_label,m_label_result,s_label_result)
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

