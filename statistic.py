#!/usr/bin/env python
import sys
import struct
from sys import argv
import csv
import argparse
import re
import json

parser = argparse.ArgumentParser(description='Statistic the pcap file.')

parser.add_argument('-r', '--read',
                    required=True, dest='file_in',
                    help='read the file')

args = parser.parse_args()
    

def statistic(fo):
    info_total = {}
    total = {}
    info_tcp = {}
    tcp = {}
    info_udp = {}
    udp = {}

    info_total.update({'type':0})
    info_tcp.update({'type':0})
    info_udp.update({'type':0})
    info_total.update({'flows':0.0})
    info_tcp.update({'flows':0.0})
    info_udp.update({'flows':0.0})
    #lines = fo.readlines
    while 1 :
        line = fo.readline()
        if not line : break
        line = line.strip('\n')
        if line == 'UDP' :
            while 1 :
                line = fo.readline()
                if line == '\n':  break
                line = line.strip('\n')
                temp = re.sub(' +',' ', line)
                temp = temp.strip().split(' ')
                key = temp[0]
                value = int(temp[6])
                if key not in total : 
                    total.update({key:value})
                    info_total['type'] = info_total['type'] + 1 
                else : total[key] = total[key] + value
                udp.update({key:value})
                info_udp['type'] = info_udp['type'] + 1 
                info_udp['flows'] = info_udp['flows'] + value
                info_total['flows'] = info_total['flows'] + value
              
        if line == 'TCP' :
            while 1 :
                line = fo.readline()
                if not line : break
                if line == '\n':  break
                line = line.strip('\n')
                temp = re.sub(' +',' ', line)
                temp = temp.strip().split(' ')
                key = temp[0]
                value = int(temp[6])
                if key not in total : 
                    total.update({key:value})
                    info_total['type'] = info_total['type'] + 1 
                else : total[key] = total[key] + value
                tcp.update({key:value})
                info_tcp['type'] = info_tcp['type'] + 1 
                info_tcp['flows'] = info_tcp['flows'] + value
                info_total['flows'] = info_total['flows'] + value

    return tcp, udp, total, info_tcp, info_udp, info_total

def arrange(table, info_table):
    flows = info_table['flows']
    for item in table :
        print "%s:%d (%.3f%%)" % ( item[0], item[1], (item[1]/flows)*100 )
    print "Total flows : ", str(flows)
    print "Total types : ", info_table['type']

def main():
    try:
        fo = open(args.file_in, 'r')
    except IOError as (errno, strerror):
        print "I/O error({0}): {1}".format(errno, strerror)
        return

    tcp, udp, total, info_tcp, info_udp, info_total = statistic(fo)
    tcp= sorted(tcp.iteritems(), key=lambda d:d[1], reverse = True)
    udp= sorted(udp.iteritems(), key=lambda d:d[1], reverse = True)
    total= sorted(total.iteritems(), key=lambda d:d[1], reverse = True)
    print "------TCP------"
    arrange(tcp, info_tcp)
    print "------UDP------"
    arrange(udp, info_udp)
    print "------Total------"
    arrange(total, info_total)

if __name__ == "__main__" :
    main()
