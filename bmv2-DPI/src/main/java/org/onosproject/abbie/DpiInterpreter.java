/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.abbie;

import com.google.common.collect.ImmutableBiMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;


import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Implementation of a BMv2 interpreter for the ecmp.json configuration.
 */
public class DpiInterpreter implements Bmv2Interpreter {

    protected static final String TABLE0 = "table0";
    protected static final String FORWARD = "forward";
    protected static final String SEND_TO_CPU = "send_to_cpu";
    protected static final String DROP = "_drop";
    protected static final String SET_EGRESS_PORT = "set_egress_port";
    protected static final String PORT = "port";
    protected static final String STANDARD_METADATA = "standard_metadata";
    protected static final String ETHERNET_HEADER = "ethernet_header";
    protected static final String INSTANCE_TYPE = "instance_type";

    protected static final String DETECT_FOUR_BYTE_PAYLOAD = "detect_four_byte_payload";
    protected static final String FOUR_BYTE_PAYLOAD = "four_byte_payload";
    protected static final String DATA = "data";
    protected static final String INTRINSIC_METADATA = "intrinsic_metadata";
    protected static final String PAYLOAD_LENGTH = "payload_len";
    protected static final String DO_SET_LABEL_BY_DETECT = "do_set_label_by_detect";
    protected static final String LABEL = "label";
    protected static final String SUB_LABEL = "sublabel";

    protected static final String LABEL_ENCUP = "label_encup";
    protected static final String LEARNING_METADATA = "learning_metadata";
    protected static final String TYPE = "_type";
    protected static final String DO_LABEL_ENCAP = "do_label_encap";

    protected static final String DETECT_DNS = "detect_dns";
    protected static final String DNS_HEADER = "dns_header";
    protected static final String DO_ASSEMBLE = "do_assemble";

    protected static final String DETECT_QUIC = "detect_quic";
    protected static final String QUIC_FLAGS = "quic_flags";
    protected static final String RESET = "reset";
    protected static final String RESERVED = "reserved";
    protected static final String NOP = "_nop";

    protected static final String SET_QUIC = "set_quic";

    protected static final String GUESS_BY_TCP_PORT = "guess_by_tcp_port";
    protected static final String GUESS_BY_UDP_PORT = "guess_by_udp_port";
    protected static final String TCP_HEADER = "tcp_header";
    protected static final String UDP_HEADER = "udp_header";
    protected static final String SRCPORT = "srcPort";
    protected static final String DSTPORT = "dstPort";
    protected static final String DO_SET_LABEL_BY_GUESS = "do_set_label_by_guess";

    protected static final String GUESS_BY_SRC_ADDRESS = "guess_by_src_address";
    protected static final String GUESS_BY_DST_ADDRESS = "guess_by_dst_address";
    protected static final String IPV4_HEADER = "ipv4_header";
    protected static final String SRCADDRESS = "srcAddr";
    protected static final String DSTADDRESS = "dstAddr";
    protected static final String DO_SET_SUB_LABEL_BY_GUESS = "do_set_sub_label_by_guess";

    protected static final String RULE_MATCH = "rule_match";
    protected static final String FIVE_TUPLE_METADATA = "five_tuple_metadata";
    protected static final String DO_SET_LABEL_BY_MATCH_RULE = "do_set_label_by_match_rule";
    protected static final String LEARNING = "learning";
    protected static final String DO_LEARNING = "do_learning";

    //protected static final String  = "";

    private static final ImmutableBiMap<Criterion.Type, String> CRITERION_TYPE_MAP = ImmutableBiMap.of(
            Criterion.Type.IN_PORT, "standard_metadata.ingress_port",
            Criterion.Type.ETH_DST, "ethernet_header.dstAddr",
            Criterion.Type.ETH_SRC, "ethernet_header.srcAddr",
            Criterion.Type.ETH_TYPE, "ethernet_header.etherType");

    private static final ImmutableBiMap<Integer, String> TABLE_ID_MAP = ImmutableBiMap.<Integer, String>builder()
            .put(0, TABLE0)
            .put(1, DETECT_FOUR_BYTE_PAYLOAD)
            .put(2, LABEL_ENCUP)
            .put(3, RULE_MATCH)
            .put(4, DETECT_DNS)
            .put(5, DETECT_QUIC)
            .put(6, SET_QUIC)
            .put(7, GUESS_BY_DST_ADDRESS)
            .put(8, GUESS_BY_SRC_ADDRESS)
            .put(9, GUESS_BY_TCP_PORT)
            .put(10, GUESS_BY_UDP_PORT)
            .put(11, LEARNING)
            .put(12, FORWARD)
            .build();

    @Override
    public ImmutableBiMap<Integer, String> tableIdMap() {
        return TABLE_ID_MAP;
    }

    @Override
    public ImmutableBiMap<Criterion.Type, String> criterionTypeMap() {
        return CRITERION_TYPE_MAP;
    }

    @Override
    public Bmv2Action mapTreatment(TrafficTreatment treatment, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        if (treatment.allInstructions().size() == 0) {
            // No instructions means drop for us.
            return actionWithName(DROP);
        } else if (treatment.allInstructions().size() > 1) {
            // Otherwise, we understand treatments with only 1 instruction.
            throw new Bmv2InterpreterException("Treatment has multiple instructions");
        }

        Instruction instruction = treatment.allInstructions().get(0);

        switch (instruction.type()) {
            case OUTPUT:
                OutputInstruction outInstruction = (OutputInstruction) instruction;
                PortNumber port = outInstruction.port();
                if (!port.isLogical()) {
                    return buildEgressAction(port, configuration);
                } else if (port.equals(CONTROLLER)) {
                    return actionWithName(SEND_TO_CPU);
                } else {
                    throw new Bmv2InterpreterException("Egress on logical port not supported: " + port);
                }
            case NOACTION:
                return actionWithName(DROP);
            default:
                throw new Bmv2InterpreterException("Instruction type not supported: " + instruction.type().name());
        }
    }

    private static Bmv2Action buildEgressAction(PortNumber port, Bmv2Configuration configuration)
            throws Bmv2InterpreterException {

        int portBitWidth = configuration.action(SET_EGRESS_PORT).runtimeData(PORT).bitWidth();

        try {
            ImmutableByteSequence portBs = fitByteSequence(ImmutableByteSequence.copyFrom(port.toLong()), portBitWidth);
            return Bmv2Action.builder()
                    .withName(SET_EGRESS_PORT)
                    .addParameter(portBs)
                    .build();
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            throw new Bmv2InterpreterException(e.getMessage());
        }
    }


    private static Bmv2Action actionWithName(String name) {
        return Bmv2Action.builder().withName(name).build();
    }
}
