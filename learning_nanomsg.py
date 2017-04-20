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
    (PACKET_IN, PACKET_OUT,
     PARSER_START, PARSER_DONE, PARSER_EXTRACT,
     DEPARSER_START, DEPARSER_DONE, DEPARSER_EMIT,
     CHECKSUM_UPDATE,
     PIPELINE_START, PIPELINE_DONE,
     CONDITION_EVAL, TABLE_HIT, TABLE_MISS,
     ACTION_EXECUTE) = range(15)
    CONFIG_CHANGE = 999

    @staticmethod
    def get_msg_class(type_):
        classes = {
            MSG_TYPES.PACKET_IN: PacketIn,
            MSG_TYPES.PACKET_OUT: PacketOut,
            MSG_TYPES.PARSER_START: ParserStart,
            MSG_TYPES.PARSER_DONE: ParserDone,
            MSG_TYPES.PARSER_EXTRACT: ParserExtract,
            MSG_TYPES.DEPARSER_START: DeparserStart,
            MSG_TYPES.DEPARSER_DONE: DeparserDone,
            MSG_TYPES.DEPARSER_EMIT: DeparserEmit,
            MSG_TYPES.CHECKSUM_UPDATE: ChecksumUpdate,
            MSG_TYPES.PIPELINE_START: PipelineStart,
            MSG_TYPES.PIPELINE_DONE: PipelineDone,
            MSG_TYPES.CONDITION_EVAL: ConditionEval,
            MSG_TYPES.TABLE_HIT: TableHit,
            MSG_TYPES.TABLE_MISS: TableMiss,
            MSG_TYPES.ACTION_EXECUTE: ActionExecute,
            MSG_TYPES.CONFIG_CHANGE: ConfigChange,
        }
        return classes[type_]

    @staticmethod
    def get_str(type_):
        strs = {
            MSG_TYPES.PACKET_IN: "PACKET_IN",
            MSG_TYPES.PACKET_OUT: "PACKET_OUT",
            MSG_TYPES.PARSER_START: "PARSER_START",
            MSG_TYPES.PARSER_DONE: "PARSER_DONE",
            MSG_TYPES.PARSER_EXTRACT: "PARSER_EXTRACT",
            MSG_TYPES.DEPARSER_START: "DEPARSER_START",
            MSG_TYPES.DEPARSER_DONE: "DEPARSER_DONE",
            MSG_TYPES.DEPARSER_EMIT: "DEPARSER_EMIT",
            MSG_TYPES.CHECKSUM_UPDATE: "CHECKSUM_UPDATE",
            MSG_TYPES.PIPELINE_START: "PIPELINE_START",
            MSG_TYPES.PIPELINE_DONE: "PIPELINE_DONE",
            MSG_TYPES.CONDITION_EVAL: "CONDITION_EVAL",
            MSG_TYPES.TABLE_HIT: "TABLE_HIT",
            MSG_TYPES.TABLE_MISS: "TABLE_MISS",
            MSG_TYPES.ACTION_EXECUTE: "ACTION_EXECUTE",
            MSG_TYPES.CONFIG_CHANGE: "CONFIG_CHANGE",
        }
        return strs[type_]

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
        return "type: %s, switch_id: %d, cxt_id: %d, sig: %d, " \
            "id: %d, copy_id: %d" %\
            (self.type_str, self.switch_id, self.cxt_id,
             self.sig, self.id_, self.copy_id)

class Learning(Msg):
    def __init__(self, msg):
        super(ConfigChange, self).__init__(msg)
        self.type_ = MSG_TYPES.CONFIG_CHANGE
        self.type_str = MSG_TYPES.get_str(self.type_)
        self.struct_ = struct.Struct("")

    def extract(self):
        super(ConfigChange, self).extract()

    def __str__(self):
        return "type: %s, switch_id: %d" % (self.type_str, self.switch_id)

def json_init(client):
    json_cfg = utils.get_json_config(standard_client=client)
    name_map.load_names(json_cfg)

def recv_msgs(socket_addr, client):
    def get_msg_type(msg):
        type_, = struct.unpack('i', msg[:4])
        return type_

    json_init(client)

    sub = nnpy.Socket(nnpy.AF_SP, nnpy.SUB)
    sub.connect(socket_addr)
    sub.setsockopt(nnpy.SUB, nnpy.SUB_SUBSCRIBE, '')
    while True:
        msg = sub.recv()
        msg_type = get_msg_type(msg)
        length = len(msg) 
        #switch_id, cxt_id, list_id, buffer_id, num_samples = struct.unpack("<iiiQI", msg )
        one = struct.unpack( "%dc" % length, msg[:length] )
        print one       
        '''
        try:
            p = MSG_TYPES.get_msg_class(msg_type)(msg)
        except:
            print "Unknown msg type", msg_type
            continue
        p.extract()
        print p
        '''

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
