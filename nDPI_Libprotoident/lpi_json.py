

#from dpkt import pcap, ssl
from subprocess import call
from os import devnull
import os
import warnings
#import mpkt
import json
import sys
import csv

def file2dict(appfile, fo_writer):
    table = {}
    for line in appfile:
        data = line.split(' ')[:6]
        label = data[0]
        src = "%s:%s" % (data[1], data[3])
        dst = "%s:%s" % (data[2], data[4])
        if data[5] == '6' : protocol = "TCP"
        else : protocol = "UDP"
        key = frozenset({src, dst, protocol})
        table.update({key:label})
        print key, label
        dict2csv( fo_writer, src, dst, protocol, label )

    return table ;


def dict2csv( fo_writer, src, dst, protocol, label ):
    result = []
    result.append(src)
    result.append(dst)
    result.append(protocol)
    result.append(label)
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

    appdict = file2dict(fi, fo_writer)

    fi.close()
    fo.close()

if __name__ == "__main__":
    main()
