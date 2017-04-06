/*
Forward and Mirror
*/


/* Parser */
parser start {
    return select(current(0, 64)) {
        0 : parse_label_header;
        default: parse_ethernet;
    }
}

#define ETHERTYPE_IPV4          0x0800
#define ETHERTYPE_IPV6          0x86dd
#define IP_PROTOCOLS_TCP        6
#define IP_PROTOCOLS_UDP        17

parser parse_ethernet {
    extract(ethernet_header);
    return select(latest.etherType) {
        ETHERTYPE_IPV4 : parse_ipv4;
        ETHERTYPE_IPV6 : parse_ipv6;
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

parser parse_ipv6 {
    extract(ipv6_header);
    return select(latest.nextHdr) {
        IP_PROTOCOLS_TCP : parse_tcp;
        IP_PROTOCOLS_UDP : parse_udp;
        default : ingress;
    }
}

parser parse_tcp {
    extract(tcp_header);
    set_metadata( intrinsic_metadata.payload_len, ipv4_header.totalLen - 40 );
    return select( intrinsic_metadata.payload_len ){
        0 : ingress ;
        1 : ingress ;
        2 : ingress ;
        3 : ingress ;
        default : parse_payload;
    }
}

parser parse_udp {
    extract(udp_header);
    set_metadata( intrinsic_metadata.payload_len, udp_header.length_ - 8 );
    return select( intrinsic_metadata.payload_len ){
        0 : ingress ;
        1 : ingress ;
        2 : ingress ;
        3 : ingress ;
        default : parse_payload;
    }
}

parser parse_payload {
    extract(payload_data);
    return ingress;
}

parser parse_label_header {
    extract(label_header);
    return parse_ethernet;
}

