#!/usr/bin/env python
from mininet.net import Mininet, VERSION
from mininet.log import setLogLevel, info, debug
from mininet.cli import CLI
from distutils.version import StrictVersion
from p4_mininet import P4Switch, P4Host
from time import sleep
import sys


SW_PATH='/home/abbie/bmv2/targets/simple_switch/simple_switch'
JSON_PATH='copy.json'


def main():
    net = Mininet(controller = None, autoSetMacs=True, autoStaticArp=True)

    h1 = net.addHost('h1', cls=P4Host)
    h2 = net.addHost('h2', cls=P4Host)
    h3 = net.addHost('h3', cls=P4Host)

    s1 = net.addSwitch('s1', cls = P4Switch, sw_path=SW_PATH, json_path=JSON_PATH, thrift_port=9090)

    net.addLink(s1, h1, port1=1, port2=1)
    net.addLink(s1, h2, port1=2, port2=1)
    net.addLink(s1, h3, port1=3, port2=1)



    net.start()
    CLI(net)
    net.stop()

if __name__ == '__main__':
    setLogLevel('debug')
    main()

