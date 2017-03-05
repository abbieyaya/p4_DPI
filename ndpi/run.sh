#!/bin/bash
# Program:
#       Use loop to run ndpi

while [ 1 ]
do
        sudo ./ndpiReader -i ens33 -j ndpi_out.json -v 1 -f tcp -s 30
        python ndpi_result.py ndpi_out.json result.csv
done

