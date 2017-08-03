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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
//import org.apache.commons.lang3.tuple.Pair;
//import com.sun.org.apache.regexp.internal.RE;
import org.apache.felix.scr.annotations.Component;
//import org.onlab.packet.TCP;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
//import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
//import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
//import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.Topology;
//import org.onosproject.net.topology.TopologyGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
//import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import java.util.stream.Collectors;

//import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onosproject.abbie.DpiInterpreter.*;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;

/**
 * Implementation of an upgradable fabric app for the ECMP configuration.
 */
@Component(immediate = true)
public class DpiApp extends Abstract {

    private static final String APP_NAME = "org.onosproject.bmv2-DPI";
    private static final String MODEL_NAME = "DPI";
    private static final String JSON_CONFIG_PATH = "/dpi.json";
    private static final Bmv2Configuration DPI_CONFIGURATION = loadConfiguration();
    private static final DpiInterpreter DPI_INTERPRETER = new DpiInterpreter();
    protected static final Bmv2DeviceContext DPI_CONTEXT =
            new Bmv2DeviceContext(DPI_CONFIGURATION, DPI_INTERPRETER);

    private static final Map<DeviceId, Map<Set<PortNumber>, Short>> DEVICE_GROUP_ID_MAP = Maps.newHashMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Bmv2Controller bmv2Controller;


    public DpiApp() {
        super(APP_NAME, MODEL_NAME, DPI_CONTEXT);
    }

    @Override
    public boolean initDevice(DeviceId deviceId) {
        // Nothing to do.
        return true;
    }

    @Override
    public List<FlowRule> generateLeafRules(DeviceId leaf, Host srcHost, Collection<Host> dstHosts,
                                            Collection<DeviceId> availableSpines, Topology topo)
            throws FlowRuleGeneratorException {
        /*
        // Get ports which connect this leaf switch to hosts.
        Set<PortNumber> hostPorts = deviceService.getPorts(leaf)
                .stream()
                .filter(port -> !isFabricPort(port, topo))
                .map(Port::number)
                .collect(Collectors.toSet());

        // Get ports which connect this leaf to the given available spines.
        TopologyGraph graph = topologyService.getGraph(topo);
        Set<PortNumber> fabricPorts = graph.getEdgesFrom(new DefaultTopologyVertex(leaf))
                .stream()
                .filter(e -> availableSpines.contains(e.dst().deviceId()))
                .map(e -> e.link().src().port())
                .collect(Collectors.toSet());

        PortNumber hostPort = hostPorts.iterator().next();

        List<FlowRule> rules = Lists.newArrayList();

        // From srHost to dstHosts.
        for (Host dstHost : dstHosts) {
            FlowRule rule = flowRuleBuilder(leaf, TABLE0)
                    .withSelector(
                            DefaultTrafficSelector.builder()
                                    //.matchInPort(srcHost.location().port())
                                    .matchEthSrc(srcHost.mac())
                                    .matchEthDst(dstHost.mac())
                                    .build())
                    .withTreatment(
                            DefaultTrafficTreatment.builder()
                                    .setOutput(dstHost.location().port())
                                    .build())
                    .build();
            rules.add(rule);

        }
        */

        List<FlowRule> rules = Lists.newArrayList();

        rules.add(buildForwardRule(leaf, 1, 1));
        rules.add(buildForwardRule(leaf, 2, 2));
        rules.add(buildForwardRule(leaf, 3, 3));

        return rules;
    }

    @Override
    public List<FlowRule> generateFourByteRules(DeviceId leaf)
            throws FlowRuleGeneratorException {

        List<FlowRule> rules = Lists.newArrayList();

        rules.add(buildFourByteRule(leaf, 0x00000200, 0x0000ff00, 0x0000, 0x0000, 1, 0));

        return rules;
    }

    @Override
    public List<FlowRule> generateLabelEncupRules(DeviceId leaf)
            throws FlowRuleGeneratorException {

        List<FlowRule> rules = Lists.newArrayList();

        ExtensionSelector extSelector = buildLabelEncupSelector(1);
        ExtensionTreatment extTreatment = buildLabelEncupTreatment();
        FlowRule rule = flowRuleBuilder(leaf, LABEL_ENCUP)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();
        rules.add(rule);

        return rules;
    }


    @Override
    public List<FlowRule> generateDetectQuicRules(DeviceId leaf)
            throws FlowRuleGeneratorException {

        List<FlowRule> rules = Lists.newArrayList();

        ExtensionSelector extSelector =
                Bmv2ExtensionSelector.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .matchExact(QUIC_FLAGS, RESET, 0)
                        .matchExact(QUIC_FLAGS, RESERVED, 0)
                        .build();

        ExtensionTreatment extTreatment =
                Bmv2ExtensionTreatment.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .setActionName(NOP)
                        .build();

        FlowRule rule = flowRuleBuilder(leaf, DETECT_QUIC)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();
        rules.add(rule);

        return rules;
    }

    @Override
    public boolean generateDefaultRules(DeviceId leaf)
            throws Bmv2RuntimeException {
        try {
            Bmv2DeviceAgent agent = bmv2Controller.getAgent(leaf);
            agent.setTableDefaultAction(SET_QUIC, buildSetQuicAction(DPI_CONFIGURATION, 3, 0));
            //agent.setTableDefaultAction(DETECT_DNS, buildDetectDnsAction());
            agent.setTableDefaultAction(LEARNING, buildLearningAction());
            agent.setTableDefaultAction(FORWARD, buildForwardAction(DPI_CONFIGURATION, 4));
            return true;
        } catch (Bmv2RuntimeException e) {
            log.debug("Exception while initializing device {}: {}", leaf, e.explain());
            return false;
        }
    }

    @Override
    public List<FlowRule> generateGuessByPortRulesPart1(DeviceId leaf)
            throws FlowRuleGeneratorException {
        List<FlowRule> rules = Lists.newArrayList();

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0035, 0xffff, 0x0000, 0x0000, 2));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0035, 0xffff, 2));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0035, 0xffff, 0x0000, 0x0000, 2));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0035, 0xffff, 2));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0015, 0xffff, 0x0000, 0x0000, 8));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0015, 0xffff, 8));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0014, 0xffff, 0x0000, 0x0000, 9));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0014, 0xffff, 9));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x006E, 0xffff, 0x0000, 0x0000, 10));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x006E, 0xffff, 10));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x03E3, 0xffff, 0x0000, 0x0000, 11));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x03E3, 0xffff, 11));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0019, 0xffff, 0x0000, 0x0000, 12));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0019, 0xffff, 12));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x01D1, 0xffff, 0x0000, 0x0000, 13));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x01D1, 0xffff, 13));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x008F, 0xffff, 0x0000, 0x0000, 14));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x008F, 0xffff, 14));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x03E1, 0xffff, 0x0000, 0x0000, 15));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x03E1, 0xffff, 15));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x2368, 0xffff, 0x0000, 0x0000, 16));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x2368, 0xffff, 16));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x2367, 0xffff, 0x0000, 0x0000, 16));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x2367, 0xffff, 16));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x008B, 0xffff, 0x0000, 0x0000, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x008B, 0xffff, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0089, 0xffff, 0x0000, 0x0000, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0089, 0xffff, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x008A, 0xffff, 0x0000, 0x0000, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x008A, 0xffff, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x008B, 0xffff, 0x0000, 0x0000, 17));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x008B, 0xffff, 17));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0801, 0xffff, 0x0000, 0x0000, 18));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0801, 0xffff, 18));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0801, 0xffff, 0x0000, 0x0000, 18));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0801, 0xffff, 18));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0A2D, 0xffff, 0x0000, 0x0000, 19));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0A2D, 0xffff, 19));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x00B1, 0xffff, 0x0000, 0x0000, 20));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x00B1, 0xffff, 20));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x00B1, 0xffff, 0x0000, 0x0000, 20));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x00B1, 0xffff, 20));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x01BD, 0xffff, 0x0000, 0x0000, 21));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x01BD, 0xffff, 21));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0202, 0xffff, 0x0000, 0x0000, 22));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0202, 0xffff, 22));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0202, 0xffff, 0x0000, 0x0000, 22));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0202, 0xffff, 22));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x1538, 0xffff, 0x0000, 0x0000, 23));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x1538, 0xffff, 23));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0CEA, 0xffff, 0x0000, 0x0000, 24));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0CEA, 0xffff, 24));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0387, 0xffff, 0x0000, 0x0000, 25));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0387, 0xffff, 25));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0386, 0xffff, 0x0000, 0x0000, 25));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0386, 0xffff, 25));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0387, 0xffff, 0x0000, 0x0000, 25));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0387, 0xffff, 25));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0xC8D5, 0xffff, 0x0000, 0x0000, 26));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0xC8D5, 0xffff, 26));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0xC8D5, 0xffff, 0x0000, 0x0000, 26));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0xC8D5, 0xffff, 26));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1A73, 0xffff, 0x0000, 0x0000, 26));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1A73, 0xffff, 26));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x022A, 0xffff, 0x0000, 0x0000, 27));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x022A, 0xffff, 27));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x022A, 0xffff, 0x0000, 0x0000, 27));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x022A, 0xffff, 27));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x00C2, 0xffff, 0x0000, 0x0000, 28));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x00C2, 0xffff, 28));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x00C2, 0xffff, 0x0000, 0x0000, 28));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x00C2, 0xffff, 28));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0017, 0xffff, 0x0000, 0x0000, 29));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0017, 0xffff, 29));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x01F4, 0xffff, 0x0000, 0x0000, 30));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x01F4, 0xffff, 30));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x01F4, 0xffff, 0x0000, 0x0000, 30));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x01F4, 0xffff, 30));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1194, 0xffff, 0x0000, 0x0000, 30));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1194, 0xffff, 30));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0A2C, 0xffff, 0x0000, 0x0000, 31));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0A2C, 0xffff, 31));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0D3D, 0xffff, 0x0000, 0x0000, 32));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0D3D, 0xffff, 32));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x170C, 0xffff, 0x0000, 0x0000, 33));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x170C, 0xffff, 33));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x170D, 0xffff, 0x0000, 0x0000, 33));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x170D, 0xffff, 33));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x16A8, 0xffff, 0x0000, 0x0000, 33));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x16A8, 0xffff, 33));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0016, 0xffff, 0x0000, 0x0000, 34));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0016, 0xffff, 34));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x11D9, 0xffff, 0x0000, 0x0000, 35));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x11D9, 0xffff, 35));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x11D9, 0xffff, 0x0000, 0x0000, 35));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x11D9, 0xffff, 35));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0224, 0xffff, 0x0000, 0x0000, 36));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0224, 0xffff, 36));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0224, 0xffff, 0x0000, 0x0000, 36));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0224, 0xffff, 36));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0058, 0xffff, 0x0000, 0x0000, 37));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0058, 0xffff, 37));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0058, 0xffff, 0x0000, 0x0000, 37));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0058, 0xffff, 37));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x13C4, 0xffff, 0x0000, 0x0000, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x13C4, 0xffff, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x13C5, 0xffff, 0x0000, 0x0000, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x13C5, 0xffff, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x13C4, 0xffff, 0x0000, 0x0000, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x13C4, 0xffff, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x13C5, 0xffff, 0x0000, 0x0000, 38));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x13C5, 0xffff, 38));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0185, 0xffff, 0x0000, 0x0000, 39));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0185, 0xffff, 39));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0185, 0xffff, 0x0000, 0x0000, 39));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0185, 0xffff, 39));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0599, 0xffff, 0x0000, 0x0000, 40));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0599, 0xffff, 40));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x059A, 0xffff, 0x0000, 0x0000, 40));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x059A, 0xffff, 40));



        return rules;
    }

    @Override
    public List<FlowRule> generateGuessByPortRulesPart2(DeviceId leaf)
            throws FlowRuleGeneratorException {
        List<FlowRule> rules = Lists.newArrayList();

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0087, 0xffff, 0x0000, 0x0000, 41));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0087, 0xffff, 41));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x1F90, 0xffff, 0x0000, 0x0000, 42));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x1F90, 0xffff, 42));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0C38, 0xffff, 0x0000, 0x0000, 42));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0C38, 0xffff, 42));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0548, 0xffff, 0x0000, 0x0000, 43));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0548, 0xffff, 43));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x05D6, 0xffff, 0x0000, 0x0000, 44));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x05D6, 0xffff, 44));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0A26, 0xffff, 0x0000, 0x0000, 44));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0A26, 0xffff, 44));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0714, 0xffff, 0x0000, 0x0000, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0714, 0xffff, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0715, 0xffff, 0x0000, 0x0000, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0715, 0xffff, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0714, 0xffff, 0x0000, 0x0000, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0714, 0xffff, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0715, 0xffff, 0x0000, 0x0000, 45));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0715, 0xffff, 45));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0C81, 0xffff, 0x0000, 0x0000, 46));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0C81, 0xffff, 46));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x06F4, 0xffff, 0x0000, 0x0000, 47));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x06F4, 0xffff, 47));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x076C, 0xffff, 0x0000, 0x0000, 47));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x076C, 0xffff, 47));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x14EB, 0xffff, 0x0000, 0x0000, 48));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x14EB, 0xffff, 48));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x14EB, 0xffff, 0x0000, 0x0000, 48));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x14EB, 0xffff, 48));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x17BD, 0xffff, 0x0000, 0x0000, 49));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x17BD, 0xffff, 49));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x17BE, 0xffff, 0x0000, 0x0000, 49));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x17BE, 0xffff, 49));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x04AA, 0xffff, 0x0000, 0x0000, 50));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x04AA, 0xffff, 50));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x04AA, 0xffff, 0x0000, 0x0000, 50));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x04AA, 0xffff, 50));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x06B7, 0xffff, 0x0000, 0x0000, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x06B7, 0xffff, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x06B8, 0xffff, 0x0000, 0x0000, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x06B8, 0xffff, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x06B7, 0xffff, 0x0000, 0x0000, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x06B7, 0xffff, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x06B8, 0xffff, 0x0000, 0x0000, 51));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x06B8, 0xffff, 51));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x2710, 0xffff, 0x0000, 0x0000, 52));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x2710, 0xffff, 52));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x2710, 0xffff, 0x0000, 0x0000, 52));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x2710, 0xffff, 52));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x07D0, 0xffff, 0x0000, 0x0000, 53));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x07D0, 0xffff, 53));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0369, 0xffff, 0x0000, 0x0000, 54));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0369, 0xffff, 54));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x05F1, 0xffff, 0x0000, 0x0000, 55));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x05F1, 0xffff, 55));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x002B, 0xffff, 0x0000, 0x0000, 56));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x002B, 0xffff, 56));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x10F7, 0xffff, 0x0000, 0x0000, 56));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x10F7, 0xffff, 56));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0438, 0xffff, 0x0000, 0x0000, 57));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0438, 0xffff, 57));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0438, 0xffff, 0x0000, 0x0000, 57));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0438, 0xffff, 57));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x078F, 0xffff, 0x0000, 0x0000, 58));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x078F, 0xffff, 58));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x18EB, 0xffff, 0x0000, 0x0000, 59));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x18EB, 0xffff, 59));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x045F, 0xffff, 0x0000, 0x0000, 60));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x045F, 0xffff, 60));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x045F, 0xffff, 0x0000, 0x0000, 60));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x045F, 0xffff, 60));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x075B, 0xffff, 0x0000, 0x0000, 61));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x075B, 0xffff, 61));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x22B3, 0xffff, 0x0000, 0x0000, 61));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x22B3, 0xffff, 61));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x24CA, 0xffff, 0x0000, 0x0000, 62));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x24CA, 0xffff, 62));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x14E9, 0xffff, 0x0000, 0x0000, 63));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x14E9, 0xffff, 63));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x14EA, 0xffff, 0x0000, 0x0000, 63));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x14EA, 0xffff, 63));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x007B, 0xffff, 0x0000, 0x0000, 64));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x007B, 0xffff, 64));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x00A1, 0xffff, 0x0000, 0x0000, 65));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x00A1, 0xffff, 65));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x00A2, 0xffff, 0x0000, 0x0000, 65));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x00A2, 0xffff, 65));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0043, 0xffff, 0x0000, 0x0000, 66));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0043, 0xffff, 66));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0044, 0xffff, 0x0000, 0x0000, 66));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0044, 0xffff, 66));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0DD8, 0xffff, 0x0000, 0x0000, 67));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0DD8, 0xffff, 67));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x13D0, 0xffff, 0x0000, 0x0000, 68));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x13D0, 0xffff, 68));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0D96, 0xffff, 0x0000, 0x0000, 69));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0D96, 0xffff, 69));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0807, 0xffff, 0x0000, 0x0000, 70));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0807, 0xffff, 70));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x18C7, 0xffff, 0x0000, 0x0000, 71));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x18C7, 0xffff, 71));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0868, 0xffff, 0x0000, 0x0000, 72));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0868, 0xffff, 72));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0869, 0xffff, 0x0000, 0x0000, 72));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0869, 0xffff, 72));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x445C, 0xffff, 0x0000, 0x0000, 73));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x445C, 0xffff, 73));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1770, 0xffff, 0x0000, 0x0000, 74));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1770, 0xffff, 74));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x64E2, 0xffff, 0x0000, 0x0000, 75));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x64E2, 0xffff, 75));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0045, 0xffff, 0x0000, 0x0000, 76));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0045, 0xffff, 76));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0B80, 0xffff, 0x0000, 0x0000, 77));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x0B80, 0xffff, 77));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0xE39B, 0xffff, 0x0000, 0x0000, 78));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0xE39B, 0xffff, 78));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x2711, 0xffff, 0x0000, 0x0000, 79));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x2711, 0xffff, 79));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1F31, 0xffff, 0x0000, 0x0000, 80));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1F31, 0xffff, 80));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1F33, 0xffff, 0x0000, 0x0000, 80));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1F33, 0xffff, 80));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1633, 0xffff, 0x0000, 0x0000, 81));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1633, 0xffff, 81));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x1634, 0xffff, 0x0000, 0x0000, 81));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x1634, 0xffff, 81));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x21A4, 0xffff, 0x0000, 0x0000, 82));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_UDP_PORT, UDP_HEADER, 0x0000, 0x0000, 0x21A4, 0xffff, 82));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0050, 0xffff, 0x0000, 0x0000, 83));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0050, 0xffff, 83));

        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x01BB, 0xffff, 0x0000, 0x0000, 90));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x01BB, 0xffff, 90));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0BB9, 0xffff, 0x0000, 0x0000, 90));
        rules.add(buildGuessByPortRule(leaf, GUESS_BY_TCP_PORT, TCP_HEADER, 0x0000, 0x0000, 0x0BB9, 0xffff, 90));

        return rules;
    }

    @Override
    public List<FlowRule> generateGuessByAddressRules(DeviceId leaf)
            throws FlowRuleGeneratorException {
        List<FlowRule> rules = Lists.newArrayList();

        generateGoogleRules(leaf, rules);
        generateOtherRules1(leaf, rules);
        generateOtherRules2(leaf, rules);


        log.info("Address RULES {}", rules.size());

        return rules;
    }

    public void generateGoogleRules(DeviceId leaf, List<FlowRule> rules)
            throws FlowRuleGeneratorException {
        buildGuessByAddressRule(leaf, 0x01000000, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x01010100, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x01020300, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x08063000, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0x08080400, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x08080800, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x080FCA00, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x0822D000, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0x0822D800, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0x0823C000, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0x0823C800, 21, 13, rules);

        buildGuessByAddressRule(leaf, 0x17EC3000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0x17FB8000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x23B80000, 13, 13, rules);
        buildGuessByAddressRule(leaf, 0x2D79E400, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x2D79E500, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x2D79E600, 23, 13, rules);
        buildGuessByAddressRule(leaf, 0x2E1CF700, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x4009E000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x400F7000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0x40E9A000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x42660000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0x42F94000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x46208000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x480EC000, 18, 13, rules);
        buildGuessByAddressRule(leaf, 0x4A721800, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0x4A7D0000, 16, 13, rules);
        buildGuessByAddressRule(leaf, 0x59CFE000, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0x673E4000, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x673E4100, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0x673E4200, 23, 13, rules);
        buildGuessByAddressRule(leaf, 0x68840000, 14, 13, rules);
        buildGuessByAddressRule(leaf, 0x689A0000, 15, 13, rules);
        buildGuessByAddressRule(leaf, 0x68C40000, 14, 13, rules);
        buildGuessByAddressRule(leaf, 0x68EDA000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x6BA7A000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0x6BB2C000, 18, 13, rules);
        buildGuessByAddressRule(leaf, 0x6C3B5000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0x6CAAC000, 18, 13, rules);
        buildGuessByAddressRule(leaf, 0x6CB10000, 17, 13, rules);
        buildGuessByAddressRule(leaf, 0x71C56800, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0x82D30000, 16, 13, rules);
        buildGuessByAddressRule(leaf, 0x8EFA0000, 15, 13, rules);
        buildGuessByAddressRule(leaf, 0x92940000, 17, 13, rules);

        buildGuessByAddressRule(leaf, 0xA2D89400, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xA2DEB000, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0xAC660800, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0xAC6E2000, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0xACFD0000, 16, 13, rules);
        buildGuessByAddressRule(leaf, 0xADC20000, 16, 13, rules);
        buildGuessByAddressRule(leaf, 0xADFF7000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0xB9191C00, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xB9969400, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xC068A000, 23, 13, rules);
        buildGuessByAddressRule(leaf, 0xC0771C00, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0xC09E1C00, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xC0B20000, 15, 13, rules);
        buildGuessByAddressRule(leaf, 0xC1210400, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0xC1210500, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0xC1C8DE00, 24, 13, rules);
        buildGuessByAddressRule(leaf, 0xC27A5000, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xC7C07000, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xC7DFE800, 21, 13, rules);
        buildGuessByAddressRule(leaf, 0xCFDFA000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0xD0419800, 22, 13, rules);
        buildGuessByAddressRule(leaf, 0xD075E000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0xD1558000, 17, 13, rules);
        buildGuessByAddressRule(leaf, 0xD16BB000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0xD83AC000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0xD8495000, 20, 13, rules);
        buildGuessByAddressRule(leaf, 0xD8EF2000, 19, 13, rules);
        buildGuessByAddressRule(leaf, 0xD8FCDC00, 22, 13, rules);

    }

    public void generateOtherRules1(DeviceId leaf, List<FlowRule> rules)
            throws FlowRuleGeneratorException {
        /* Facebook 8 */
        buildGuessByAddressRule(leaf, 0x1F0D1800, 21, 8, rules);
        buildGuessByAddressRule(leaf, 0x1F0D4000, 18, 8, rules);
        buildGuessByAddressRule(leaf, 0x2D402800, 22, 8, rules);
        buildGuessByAddressRule(leaf, 0x42DC9000, 20, 8, rules);
        buildGuessByAddressRule(leaf, 0x453FB000, 20, 8, rules);
        buildGuessByAddressRule(leaf, 0x45ABE000, 19, 8, rules);
        buildGuessByAddressRule(leaf, 0x4A774C00, 22, 8, rules);
        buildGuessByAddressRule(leaf, 0x67046000, 22, 8, rules);
        buildGuessByAddressRule(leaf, 0x81860000, 16, 8, rules);
        buildGuessByAddressRule(leaf, 0x9DF00000, 16, 8, rules);
        buildGuessByAddressRule(leaf, 0xADFC4000, 18, 8, rules);
        buildGuessByAddressRule(leaf, 0xB33CC000, 22, 8, rules);
        buildGuessByAddressRule(leaf, 0xB93CD800, 22, 8, rules);
        buildGuessByAddressRule(leaf, 0xC7C94000, 22, 8, rules);
        buildGuessByAddressRule(leaf, 0xCC0F1400, 22, 8, rules);
        // Google -> Facebook
        buildGuessByAddressRule(leaf, 0xACD90000, 16, 8, rules);



        /* Twitter  9 */
        buildGuessByAddressRule(leaf, 0x0819C200, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0x0819C400, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0x450C3800, 21, 9, rules);
        buildGuessByAddressRule(leaf, 0x67FC7000, 22, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42800, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42900, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42A00, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42B00, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42C00, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42D00, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42E00, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0x68F42F00, 24, 9, rules);
        buildGuessByAddressRule(leaf, 0xB92D0400, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0xB92D0600, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0xBC40E000, 21, 9, rules);
        buildGuessByAddressRule(leaf, 0xC02C4400, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0xC030EC00, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0xC0854C00, 22, 9, rules);
        buildGuessByAddressRule(leaf, 0xC7109C00, 22, 9, rules);
        buildGuessByAddressRule(leaf, 0xC73B9400, 22, 9, rules);
        buildGuessByAddressRule(leaf, 0xC7453A00, 23, 9, rules);
        buildGuessByAddressRule(leaf, 0xC7603800, 21, 9, rules);



        /* Whatsapp 4 */
        buildGuessByAddressRule(leaf, 0x3216C6CC, 30, 4, rules);
        buildGuessByAddressRule(leaf, 0x4B7E2720, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0x6CA8B460, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0x9E553A00, 25, 4, rules);
        buildGuessByAddressRule(leaf, 0x9E55FE40, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xA92F2320, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xA93743E0, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xA93764A0, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xA937EBA0, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xADC0A220, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xB8AD8840, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xB93CDA35, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xC60BFB20, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xD02B73C0, 27, 4, rules);
        buildGuessByAddressRule(leaf, 0xD02B7A80, 27, 4, rules);



        /* Wechat 10 */
        buildGuessByAddressRule(leaf, 0xCBCD93AB, 32, 10, rules);
        buildGuessByAddressRule(leaf, 0xCBCD93AD, 32, 10, rules);
        buildGuessByAddressRule(leaf, 0xCBCD97A2, 32, 10, rules);
        buildGuessByAddressRule(leaf, 0x67071E25, 32, 10, rules);



        /* Netflix 11  */
        buildGuessByAddressRule(leaf, 0x17F60000, 18, 11, rules);
        buildGuessByAddressRule(leaf, 0x254DB800, 21, 11, rules);
        buildGuessByAddressRule(leaf, 0x26487E00, 24, 11, rules);
        buildGuessByAddressRule(leaf, 0x2D390000, 17, 11, rules);
        buildGuessByAddressRule(leaf, 0x40788000, 17, 11, rules);
        buildGuessByAddressRule(leaf, 0x42C58000, 17, 11, rules);
        buildGuessByAddressRule(leaf, 0x4535E000, 19, 11, rules);
        buildGuessByAddressRule(leaf, 0x6CAF2000, 20, 11, rules);
        buildGuessByAddressRule(leaf, 0xB902DC00, 22, 11, rules);
        buildGuessByAddressRule(leaf, 0xB909BC00, 22, 11, rules);
        buildGuessByAddressRule(leaf, 0xC0AD4000, 18, 11, rules);
        buildGuessByAddressRule(leaf, 0xC6266000, 19, 11, rules);
        buildGuessByAddressRule(leaf, 0xC62D3000, 20, 11, rules);
        buildGuessByAddressRule(leaf, 0xD04B4C00, 22, 11, rules);



        /* Apple 12 */
        buildGuessByAddressRule(leaf, 0x11000000, 8, 12, rules);
        buildGuessByAddressRule(leaf, 0xC0233200, 24, 12, rules);
        buildGuessByAddressRule(leaf, 0xC6B71000, 24, 12, rules);
        buildGuessByAddressRule(leaf, 0xC6B71100, 24, 12, rules);
        buildGuessByAddressRule(leaf, 0xCDB4AF00, 24, 12, rules);
    }

    public void generateOtherRules2(DeviceId leaf, List<FlowRule> rules)
            throws FlowRuleGeneratorException {

        /* Skype 1 */
        buildGuessByAddressRule(leaf, 0x9D388740, 26, 1, rules);
        buildGuessByAddressRule(leaf, 0x9D38B900, 26, 1, rules);
        buildGuessByAddressRule(leaf, 0x9D383400, 26, 1, rules);
        buildGuessByAddressRule(leaf, 0x9D383580, 25, 1, rules);
        buildGuessByAddressRule(leaf, 0x9D38C600, 26, 1, rules);
        buildGuessByAddressRule(leaf, 0x9D3C0000, 16, 1, rules);
        buildGuessByAddressRule(leaf, 0x9D360000, 15, 1, rules);
        buildGuessByAddressRule(leaf, 0x0D400000, 11, 1, rules);
        buildGuessByAddressRule(leaf, 0x0D6B0380, 32, 1, rules);
        buildGuessByAddressRule(leaf, 0x0D6B0381, 32, 1, rules);
        buildGuessByAddressRule(leaf, 0x6FDD4000, 18, 1, rules);
        buildGuessByAddressRule(leaf, 0x5BBED800, 21, 1, rules);
        buildGuessByAddressRule(leaf, 0x5BBEDA00, 24, 1, rules);
        buildGuessByAddressRule(leaf, 0x287F816D, 32, 1, rules);
        buildGuessByAddressRule(leaf, 0x4237DF00, 26, 1, rules);
        buildGuessByAddressRule(leaf, 0x17600000, 13, 1, rules);

        /* Dropbox 14 */
        buildGuessByAddressRule(leaf, 0x2D3A4000, 20, 14, rules);
        buildGuessByAddressRule(leaf, 0x6CA0A000, 20, 14, rules);
        buildGuessByAddressRule(leaf, 0xA27D0000, 16, 14, rules);
        buildGuessByAddressRule(leaf, 0xB92D0800, 22, 14, rules);
        buildGuessByAddressRule(leaf, 0xC72FD800, 22, 14, rules);


        /* BitTorrent 5 */
        buildGuessByAddressRule(leaf, 0xB9381424, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0xC0DEED0A, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0x4DDEAE14, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0x25779CBD, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0x05277C26, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0x4FC0AB43, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0xAC100010, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0xB2A4F550, 32, 5, rules);
        buildGuessByAddressRule(leaf, 0xAE597B3E, 32, 5, rules);


        /* Twitch 15 */

        buildGuessByAddressRule(leaf, 0x17A00000, 24, 15, rules);
        buildGuessByAddressRule(leaf, 0x2D718000, 22, 15, rules);
        buildGuessByAddressRule(leaf, 0x34DFC000, 20, 15, rules);
        buildGuessByAddressRule(leaf, 0x34DFD000, 21, 15, rules);
        buildGuessByAddressRule(leaf, 0x34DFD800, 21, 15, rules);
        buildGuessByAddressRule(leaf, 0x34DFE000, 20, 15, rules);
        buildGuessByAddressRule(leaf, 0x34DFF000, 20, 15, rules);
        buildGuessByAddressRule(leaf, 0x67353000, 22, 15, rules);
        buildGuessByAddressRule(leaf, 0xB92ACC00, 22, 15, rules);
        buildGuessByAddressRule(leaf, 0xC0104000, 21, 15, rules);
        buildGuessByAddressRule(leaf, 0xC06CEF00, 24, 15, rules);
        buildGuessByAddressRule(leaf, 0xC709F800, 21, 15, rules);


        /* GitHub 16 */
        buildGuessByAddressRule(leaf, 0xC01EFC00, 22, 16, rules);


        /* Steam 17 */
        buildGuessByAddressRule(leaf, 0x2D79B800, 22, 17, rules);
        buildGuessByAddressRule(leaf, 0x670A7C00, 23, 17, rules);
        buildGuessByAddressRule(leaf, 0x671C3600, 23, 17, rules);
        buildGuessByAddressRule(leaf, 0x8F899200, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0x92429800, 21, 17, rules);
        buildGuessByAddressRule(leaf, 0x99FE5600, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0x9B85E000, 19, 17, rules);
        buildGuessByAddressRule(leaf, 0xA2FEC000, 21, 17, rules);
        buildGuessByAddressRule(leaf, 0xB919B400, 22, 17, rules);
        buildGuessByAddressRule(leaf, 0xBED87900, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0xBED92100, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0xC0456000, 22, 17, rules);
        buildGuessByAddressRule(leaf, 0xCDB9C200, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0xCDC40600, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0xD040C800, 24, 17, rules);
        buildGuessByAddressRule(leaf, 0xD040C900, 22, 17, rules);
        buildGuessByAddressRule(leaf, 0xD04EA400, 22, 17, rules);

    }

    private FlowRule buildFourByteRule(DeviceId leaf,
                                       int data, int dataMask, int length, int lengthMask,
                                       int mLabel, int sLabel)
            throws FlowRuleGeneratorException {

        ExtensionSelector extSelector =
                Bmv2ExtensionSelector.builder()
                .forConfiguration(DPI_CONTEXT.configuration())
                .matchTernary(FOUR_BYTE_PAYLOAD, DATA, data, dataMask)
                .matchTernary(INTRINSIC_METADATA, PAYLOAD_LENGTH, length, lengthMask)
                .build();

        ExtensionTreatment extTreatment = buildSetLabelByDetectTreatment(mLabel, sLabel);

        FlowRule rule = flowRuleBuilder(leaf, DETECT_FOUR_BYTE_PAYLOAD)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();

        return rule;
    }

    private FlowRule buildForwardRule(DeviceId leaf, long dstAddr, int port)
            throws FlowRuleGeneratorException {

        ExtensionSelector extSelector =
                Bmv2ExtensionSelector.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .matchExact(ETHERNET_HEADER, DSTADDRESS, dstAddr)
                        .build();

        ExtensionTreatment extTreatment =
                Bmv2ExtensionTreatment.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .setActionName(SET_EGRESS_PORT)
                        .addParameter(PORT, port)
                        .build();

        FlowRule rule = flowRuleBuilder(leaf, FORWARD)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();

        return rule;
    }

    private FlowRule buildGuessByPortRule(DeviceId leaf, String table, String keyHeader,
                                       int src, int srcMask, int dst, int dstMask, int mLabel)
            throws FlowRuleGeneratorException {

        ExtensionSelector extSelector =
                Bmv2ExtensionSelector.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .matchTernary(keyHeader, SRCPORT, src, srcMask)
                        .matchTernary(keyHeader, DSTPORT, dst, dstMask)
                        .build();

        ExtensionTreatment extTreatment =
                Bmv2ExtensionTreatment.builder()
                .forConfiguration(DPI_CONTEXT.configuration())
                .setActionName(DO_SET_LABEL_BY_GUESS)
                .addParameter(LABEL, mLabel)
                .build();

        FlowRule rule = flowRuleBuilder(leaf, table)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();

        return rule;
    }

    private void buildGuessByAddressRule(DeviceId leaf, int address, int prefixLength, int sLabel, List<FlowRule> rules)
            throws FlowRuleGeneratorException {

        //List<FlowRule> rules = Lists.newArrayList();

        ExtensionSelector extSrcSelector =
                Bmv2ExtensionSelector.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .matchLpm(IPV4_HEADER, SRCADDRESS, address, prefixLength)
                        .build();

        ExtensionSelector extDstSelector =
                Bmv2ExtensionSelector.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .matchLpm(IPV4_HEADER, DSTADDRESS, address, prefixLength)
                        .build();

        ExtensionTreatment extTreatment =
                Bmv2ExtensionTreatment.builder()
                        .forConfiguration(DPI_CONTEXT.configuration())
                        .setActionName(DO_SET_SUB_LABEL_BY_GUESS)
                        .addParameter(SUB_LABEL, sLabel)
                        .build();

        FlowRule srcRule = flowRuleBuilder(leaf, GUESS_BY_SRC_ADDRESS)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extSrcSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();

        FlowRule dstRule = flowRuleBuilder(leaf, GUESS_BY_DST_ADDRESS)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .extension(extDstSelector, leaf)
                                .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .extension(extTreatment, leaf)
                                .build())
                .build();

        rules.add(srcRule);
        rules.add(dstRule);
        //return rules;
    }

    private Bmv2ExtensionTreatment buildSetLabelByDetectTreatment(int mLabel, int sLabel) {
        return Bmv2ExtensionTreatment.builder()
                .forConfiguration(DPI_CONTEXT.configuration())
                .setActionName(DO_SET_LABEL_BY_DETECT)
                .addParameter(LABEL, mLabel)
                .addParameter(SUB_LABEL, sLabel)
                .build();
    }

    private Bmv2ExtensionTreatment buildLabelEncupTreatment() {
        return Bmv2ExtensionTreatment.builder()
                .forConfiguration(DPI_CONTEXT.configuration())
                .setActionName(DO_LABEL_ENCAP)
                .build();
    }

    private Bmv2ExtensionSelector buildLabelEncupSelector(int type) {
        /*
        return Bmv2ExtensionSelector.builder()
                .forConfiguration(DPI_CONTEXT.configuration())
                .matchExact(LEARNING_METADATA, TYPE, type)
                .build();
        */
        return Bmv2ExtensionSelector.builder()
                .forConfiguration(DPI_CONTEXT.configuration())
                .matchExact(STANDARD_METADATA, INSTANCE_TYPE, type)
                .build();
    }

    private static Bmv2Action buildSetQuicAction(Bmv2Configuration configuration, int mLabel, int sLabel) {
                int mLabelBitWidth = configuration.action(DO_SET_LABEL_BY_DETECT).runtimeData(LABEL).bitWidth();
                int sLabelBitWidth = configuration.action(DO_SET_LABEL_BY_DETECT).runtimeData(SUB_LABEL).bitWidth();
                ImmutableByteSequence mLabelBs = null;
                ImmutableByteSequence sLabelBs = null;
                try {
                    mLabelBs = fitByteSequence(ImmutableByteSequence.copyFrom(mLabel), mLabelBitWidth);
                    sLabelBs = fitByteSequence(ImmutableByteSequence.copyFrom(sLabel), sLabelBitWidth);
                } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
                    e.printStackTrace();
                }
                return Bmv2Action.builder()
                        .withName(DO_SET_LABEL_BY_DETECT)
                        .addParameter(mLabelBs)
                        .addParameter(sLabelBs)
                        .build();
            }

    private static Bmv2Action buildDetectDnsAction() {
        return Bmv2Action.builder()
                .withName(DO_ASSEMBLE)
                .build();
    }

    private static Bmv2Action buildLearningAction() {
        return Bmv2Action.builder()
                .withName(DO_LEARNING)
                .build();
    }

    private static Bmv2Action buildForwardAction(Bmv2Configuration configuration, int port) {
        int portBitWidth = configuration.action(SET_EGRESS_PORT).runtimeData(PORT).bitWidth();
        ImmutableByteSequence portBs = null;
        try {
            portBs = fitByteSequence(ImmutableByteSequence.copyFrom(port), portBitWidth);
        } catch (Bmv2TranslatorUtils.ByteSequenceFitException e) {
            e.printStackTrace();
        }
        return Bmv2Action.builder()
                .withName(SET_EGRESS_PORT)
                .addParameter(portBs)
                .build();
    }
    private static Bmv2Configuration loadConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    DpiApp.class.getResourceAsStream(JSON_CONFIG_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
    }
}
