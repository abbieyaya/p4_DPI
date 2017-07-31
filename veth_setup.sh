#!/bin/bash

TOE_OPTIONS="rx tx sg tso ufo gso gro lro rxvlan txvlan rxhash" 
for TOE_OPTION in $TOE_OPTIONS; do
    echo TOE_OPTION 
    /sbin/ethtool --offload eth0 "$TOE_OPTION" off &> /dev/null
    /sbin/ethtool --offload s1-eth1 "$TOE_OPTION" off &> /dev/null
    /sbin/ethtool --offload s2-eth2 "$TOE_OPTION" off &> /dev/null
done
