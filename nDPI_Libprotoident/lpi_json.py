

#from dpkt import pcap, ssl
from subprocess import call
from os import devnull
import os
import warnings
#import mpkt
import json
import sys
import csv

def file2dict(appfile):
    table = {}
    for line in appfile:
        data = line.split(' ')[:6]
        label = data[0]
        src_ip = data[1]
        dst_ip = data[2]
        src_port = data[3]
        dst_port = data[4]
        protocol = data[5]
        key = frozenset({src_ip+":"+str(src_port), dst_ip+":"+str(dst_port), protocol})
        table.update({key:label})
        print key, label

    return table ;


def dict2csv( fo_writer, dictionary ):
    csv_header = ['src_ip']
    csv_header.append('src_port')
    csv_header.append('dst_ip')
    csv_header.append('dst_port')
    csv_header.append('protocol')
    csv_header.append('label')
    print csv_header
    fo_writer.writerow(csv_header)

    for key, value in dictionary.iteritems() :
        src = key[0].split(':')
        dst = key[1].split(':')
        result = []
        result.append(src[0] )
        result.append(src[1])
        result.append(dst[0])
        result.append(dst[1])
        result.append(key[2])
        result.append(value)
        print result 
        fo_writer.writerow(result)


def main():
    if len(sys.argv) == 2 :
        file_in = sys.argv[1]
        file_out = 'essence.csv'
    elif len(sys.argv) == 3:
        file_in = sys.argv[1]
        file_out = sys.argv[2]
    else:
        print "Usage: ", sys.argv[0], "[input.txt]", "[output.csv]"
        return


    try:
        FNULL = open(devnull, 'w')   # File of nowhere
        fi = open(file_in, 'r')
        fo = open(file_out, 'w')
        fo_writer = csv.writer(fo)
    except IOError as (errno, strerror):
        print "I/O error({0}): {1}".format(errno, strerror)
        return

    appdict = file2dict(fi)
    #print appdict
    #dict2csv( fo_writer, appdict )

    fi.close()
    fo.close()

if __name__ == "__main__":
    main()
