#!/usr/bin/env python2

# Copyright 2013-present Barefoot Networks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Antonin Bas (antonin@barefootnetworks.com)
#
#

import nnpy
import struct
import sys
import json
import argparse
import bmpy_utils as utils

parser = argparse.ArgumentParser(description='BM nanomsg event logger client')
parser.add_argument('--socket', help='IPC socket to which to subscribe',
                    action="store", required=False)
parser.add_argument('--json', help='JSON description of P4 program [deprecated]',
                    action="store", required=False)
parser.add_argument('--thrift-port', help='Thrift server port for table updates',
                    type=int, action="store", default=9090)
parser.add_argument('--thrift-ip', help='Thrift IP address for table updates',
                    type=str, action="store", default='localhost')

args = parser.parse_args()

class NameMap:
    def __init__(self):
        self.names = {}

    def load_names(self, json_cfg):
        self.names = {}
        json_ = json.loads(json_cfg)
        # special case where the switch was started with an empty config
        if len(json_.keys()) == 0:
            return

        for type_ in {"header_type", "header", "parser",
                      "deparser", "action", "pipeline", "checksum"}:
            json_list = json_[type_ + "s"]
            for obj in json_list:
                self.names[(type_, obj["id"])] = obj["name"]

        for pipeline in json_["pipelines"]:
            tables = pipeline["tables"]
            for obj in tables:
                self.names[("table", obj["id"])] = obj["name"]

            conds = pipeline["conditionals"]
            for obj in conds:
                self.names[("condition", obj["id"])] = obj["name"]

    def get_name(self, type_, id_):
        return self.names.get( (type_, id_), None )

name_map = NameMap()

def name_lookup(type_, id_):
    return name_map.get_name(type_, id_)

class MSG_TYPES:
    @staticmethod
    def get_msg_class(type_):
        classes = {
            "LEA": Learning,
            "PRT": Port_monitor,
            "AGE": Ageing,
        }
        return classes[type_]

class Msg(object):
    def __init__(self, msg):
        self.msg = msg

    def extract_hdr(self):
        # < required to prevent 8-byte alignment
        struct_ = struct.Struct("<iiiQQQ")
        (_, self.switch_id, self.cxt_id,
         self.sig, self.id_, self.copy_id) = struct_.unpack_from(self.msg)
        return struct_.size

    def extract(self):
        bytes_extracted = self.extract_hdr()
        msg_remainder = self.msg[bytes_extracted:]
        return self.struct_.unpack(msg_remainder)

    def __str__(self):
        return "type: LEARNING, switch_id: %d, cxt_id: %d, sig: %d, " \
            "id: %d, copy_id: %d" %\
            (self.switch_id, self.cxt_id, self.sig, self.id_, self.copy_id)

class Learning(Msg):
    def __init__(self, msg, length):
        self.msg = msg 
        self.length = length

    def extract(self):
        ( self.switch_id, self.cxt_id, self.list_id ) = struct.unpack('3i', self.msg[4:16])        
        ( self.buffer_id, self.num_samples, self.padding ) = struct.unpack('QI4s', self.msg[16:32])        
        #len_of_a_sample = ( length - 32 ) / self.num_samples

    def __str__(self):
        return "type: LEARNING, switch_id: %d, cxt_id: %d, list_id: %d," \
                "buffer_id: %s, num_samples: %d, padding: %s" \
                % (self.switch_id, self.cxt_id, self.list_id,
                   self.buffer_id, self.num_samples, self.padding )

    def data(self):
        now = 32 
        data_length = ( self.length - 32 ) / self.num_samples
        while now < self.length :
            end = now + data_length
            print struct.unpack( '%ds' % data_length, self.msg[now:now+data_length] )
            now = end


class Port_monitor(Msg):
    def __init__(self, msg, length):
        self.msg = msg 

    def extract(self):
        ( self.switch_id, self.num_statuses, self.padding ) = struct.unpack('<iI20s', self.msg[4:32])        

    def __str__(self):
        return "type: LEARNING, switch_id: %d, num_statuses:%d, padding: %s" \
                % (self.switch_id, self.num_statuses, self.padding )

class Ageing(Msg):
    def __init__(self, msg, length):
        self.msg = msg 
        self.length = length

    def extract(self):
        ( self.switch_id, self.cxt_id, 
          self.buffer_id, self.table_id,
          self.num_entries, self.padding ) = struct.unpack('iiQiI4s', self.msg[4:32])        

    def __str__(self):
        return "type: LEARNING, switch_id: %d, cxt_id: %d, buffer_id: %d, " \
                "table_id: %d, num_entries:%d, padding: %s" \
                % (self.switch_id, self.cxt_id, self.buffer_id, 
                        self.table_id, self.num_entries, self.padding)

    def data(self):
        now = 32 
        data_length = ( self.length - 32 ) / self.num_entries 
        while now < self.length :
            end = now + data_length
            print struct.unpack( '%ds' % data_length, self.msg[now:end] )
            now = end

def json_init(client):
    json_cfg = utils.get_json_config(standard_client=client)
    name_map.load_names(json_cfg)

def recv_msgs(socket_addr, client):
    def get_msg_type(msg):
        type_, = struct.unpack('3s', msg[:3])
        return type_

    json_init(client)

    sub = nnpy.Socket(nnpy.AF_SP, nnpy.SUB)
    sub.connect(socket_addr)
    sub.setsockopt(nnpy.SUB, nnpy.SUB_SUBSCRIBE, '')
    while True:
        msg = sub.recv()
        msg_type = get_msg_type(msg)
        print msg_type
        length = len(msg)
        #switch_id, cxt_id, list_id, buffer_id, num_samples = struct.unpack("<iiiQI", msg )
        one = struct.unpack( ">%dc" % length, msg[:length] )
        #two = struct.unpack( "I" , msg[:2] )
        print one       
        
        try:
            p = MSG_TYPES.get_msg_class(msg_type)(msg, length)
        except:
            print "Unknown msg type", msg_type
            continue
        p.extract()
        print p
        print p.data()
        

def main():
    deprecated_args = ["json"]
    for a in deprecated_args:
        if getattr(args, a) is not None:
            print "Command line option '--{}' is deprecated".format(a),
            print "and will be ignored"

    client = utils.thrift_connect_standard(args.thrift_ip, args.thrift_port)
    info = client.bm_mgmt_get_info()
    socket_addr = info.elogger_socket
    if socket_addr is None:
        print "The event logger is not enabled on the switch,",
        print "run with '--nanolog <ip addr>'"
        sys.exit(1)
    if args.socket is not None:
        socket_addr = args.socket
    else:
        print "'--socket' not provided, using", socket_addr,
        print "(obtained from switch)"

    recv_msgs(socket_addr, client)

if __name__ == "__main__":
    main()
