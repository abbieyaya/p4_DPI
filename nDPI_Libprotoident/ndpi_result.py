
from subprocess import call
from os import devnull
import os
import warnings
import json
import sys
import csv

# Convert ndpi json output to a [flow: app] dictionary
def json2dict(appjson):
    table = {}
    for flow in appjson['known.flows']:
        label = str(flow['detected.protocol.name'])
        src_ip = str(flow['host_a.name'])
        dst_ip = str(flow['host_b.name'])
        src_port = str(flow['host_a.port'])
        dst_port = str(flow['host_b.port'])
        protocol = str(flow['protocol'])
        key = frozenset({src_ip+":"+str(src_port), dst_ip+":"+str(dst_port), protocol})
        table.update({key:label})
        print key, label


    '''    
    for flow in appjson['unknown.flows']:
        if flow['protocol'] != 'TCP': continue

        name = mpkt.FiveTuple(flow['host_a.name'], flow['host_b.name'],
            flow['host_a.port'], flow['host_n.port'], 'TCP').toString()

        if name in dict:
            warnings.warn(name.toString()+' has appeared twice.')
            # if name appeared twice, then we cannot distinguish those flows

        dict[name] = flow['detected.protocol.name']
    '''
    return dict

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
	temp = key.split('>')
        src = temp[0].split(':')
        dst = temp[1].split(':')
	result = []
	result.append(src[0] )
	result.append(src[1])
	result.append(dst[0])
	result.append(dst[1])
	result.append('TCP')
        result.append(value)
	print result 
        fo_writer.writerow(result)

def main():
    # Check for arguments
    if len(sys.argv) == 2:
        file_in = sys.argv[1]
        file_out = 'essence.csv'
    elif len(sys.argv) == 3:
        file_in = sys.argv[1]
        file_out = sys.argv[2]
    else:
        print "Usage: ", sys.argv[0], "[input.json]", "[output.csv]"
        return

    # Open files for input and output
    try:
        FNULL = open(devnull, 'w')   # File of nowhere
        fi = open(file_in, 'r')
        fo = open(file_out, 'w')
        fo_writer = csv.writer(fo)
    except IOError as (errno, strerror):
        print "I/O error({0}): {1}".format(errno, strerror)
        return

    appdict = json2dict(json.load(fi, encoding='latin-1'))
    #dict2csv( fo_writer, appdict )


    fi.close()
    fo.close()


if __name__ == "__main__":
    main()
