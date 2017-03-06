/*
Forward and Mirror
*/

/* Header_type */
header_type intrinsic_metadata_t {
    fields {
        ingress_global_timestamp : 32;
        lf_field_list : 32;
        mcast_grp : 16;
        egress_rid : 16;
    }
}

header_type ethernet_header_t {
    fields {
        dstAddr : 48;
        srcAddr : 48;
        etherType : 16;
    }
}

header_type ipv4_header_t {
    fields {
        version : 4;
        ihl : 4;
        diffserv : 8;
        totalLen : 16;
        identification : 16;
        flags : 3;
        fragOffset : 13;
        ttl : 8;
        protocol : 8;
        hdrChecksum : 16;
        srcAddr : 32;
        dstAddr: 32;
    }
}

header_type tcp_header_t {
    fields {
        srcPort : 16;
        dstPort : 16;
        seqNo : 32;
        ackNo : 32;
        dataOffset : 4;
        res : 3;
        ecn : 3;
        ctrl : 6;
        window : 16;
        checksum : 16;
        urgentPtr : 16;
    }
}

header_type udp_header_t {
    fields {
        srcPort : 16;
        dstPort : 16;
        length_ : 16;
        checksum : 16;
    }
}

header_type label_header_t {
    fields {
        label: 8;
    }
}
/* Header */

header ethernet_header_t ethernet_header;
header ipv4_header_t ipv4_header;
header tcp_header_t tcp_header;
header udp_header_t udp_header;
header label_header_t label_header;
metadata intrinsic_metadata_t intrinsic_metadata;

/* Parser */
parser start {
    return parse_ethernet;
}

#define ETHERTYPE_IPV4 0x0800
#define IP_PROTOCOLS_TCP  6
#define IP_PROTOCOLS_UDP  17

parser parse_ethernet {
    extract(ethernet_header);
    return select(latest.etherType) {
        ETHERTYPE_IPV4 : parse_ipv4;
        default : ingress;
    }
}

parser parse_ipv4 {
    extract(ipv4_header);
    return select(latest.protocol) {
        IP_PROTOCOLS_TCP : parse_tcp;
        IP_PROTOCOLS_UDP : parse_udp;
        default : ingress;
    }
}

parser parse_tcp {
    extract(tcp_header);
    return ingress;
}

parser parse_udp {
    extract(udp_header);
    return ingress;
}

/* Action */
action action_forward(out_port) {
    modify_field(standard_metadata.egress_spec, out_port);
}

field_list copy_to_cpu_fields {
    standard_metadata;
}

action do_copy_to_cpu(mirror_port) {
    clone_ingress_pkt_to_egress(mirror_port, copy_to_cpu_fields);
}

action do_label_encap(label) {
    add_header(label_header);
    modify_field(label_header.label, label);
}

/* Table */
table copy_to_cpu {
    actions {do_copy_to_cpu;}
    // size : 1;
}

table classifier_tcp {
    reads {
	ipv4_header.srcAddr: exact;
	ipv4_header.dstAddr: exact;
	tcp_header.srcPort: exact;
	tcp_header.dstPort: exact;
    }

    actions { 
      do_label_encap;
    }
}

table classifier_udp {
    reads {
	ipv4_header.srcAddr: exact;
	ipv4_header.dstAddr: exact;
	udp_header.srcPort: exact;
	udp_header.dstPort: exact;
    }

    actions { 
      do_label_encap;
    }
}

table forward {
    reads {
        ipv4_header.dstAddr: exact;
    }

    actions {
        action_forward;
    }
}

/* Control */

control ingress {
    apply(copy_to_cpu);
    if( ipv4_header.protocol == IP_PROTOCOLS_TCP ) apply(classifier_tcp) ;
    else if( ipv4_header.protocol == IP_PROTOCOLS_UDP ) apply(classifier_udp) ;
    apply(forward);
}

control egress {
}
