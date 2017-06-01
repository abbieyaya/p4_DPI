
from os import devnull
import csv
import os
import sys

def printTable(table):
    for key, value in table.iteritems() :
        print key, value

def csv2table(file_in):
    csvReader = csv.reader(file_in)
    table = {}
    for row in csvReader :
        src = row[0]
        dst = row[1]
        protocol = row[2]
        label = row[3]
        key = frozenset({src, dst, protocol})
        if key not in table : table.update({key:label})
        else : table[key] = label
    return table 

def hitRate(table_p4, table_nDPI):
    hit = 0.0
    miss = 0.0
    unknown = 0.0
    for key, value in table_p4.iteritems() :
        if key in table_nDPI :
            if value == table_nDPI[key] : 
                hit = hit + 1
            else : 
                print key, value, table_nDPI[key] 
                miss = miss + 1
            del table_nDPI[key]
        else :
            print( '(Miss)'),
            print key, value
            unknown = unknown + 1 

    print "------------------------Remain of nDPI-------------------------"
    printTable(table_nDPI)
    print "flow num : hit(%d) miss(%d) unknown(%d) remain(%d)" % ( hit, miss, unknown, len(table_nDPI) )
    print "Hit Rate : %.2f" % ((hit/len(table_p4))*100)
    print "Miss Rate : %.2f" % ((miss/len(table_p4))*100)
    print "Unknown Rate : %.2f" % ((unknown/len(table_p4))*100)

def main():
    if len(sys.argv) == 2 :
        file_p4 = sys.argv[1]
        file_nDPI = sys.argv[2]
    elif len(sys.argv) == 3 : 
        file_p4 = sys.argv[1]
        file_nDPI = sys.argv[2]
    else :
        print "Usage: ", argv[0], "[P4_input]", "[nDPI_input]" 

    try :
        FNULL = open(devnull, 'w') # File of nowhere
        fi_p4 = open(file_p4, 'r')
        fi_nDPI = open(file_nDPI, 'r')
    except IOError as (errno, strerror):
        print "I/O error({0}): {1}".format(errno, strerror)
        return

    # Load to table
    #print "-------------P4---------------"
    table_P4 = csv2table(fi_p4)
    #print table_P4
    #print "-------------nDPI---------------"
    table_nDPI = csv2table(fi_nDPI)
    #print table_nDPI

    # 1. check from P4
    hitRate(table_P4, table_nDPI)

    # 2. remain of nDPI and Lib
    #print printTable(table_nDPI)


if __name__ == "__main__" :
    main()
