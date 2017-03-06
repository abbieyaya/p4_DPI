#!/bin/bash
# Program:
#       Use loop to run ndpi

while [ 1 ]
do
        sudo ./ndpiReader -i h2-eth1 -j ndpi_out.json -v 1 -f tcp -s 20
        #sudo ./ndpiReader -i h2-eth1 -j ndpi_out.json -v 1 -f tcp -s 20
        python ndpi_result.py ndpi_out.json result.csv
done

