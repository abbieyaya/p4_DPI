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
    set_metadata( intrinsic_metadata.srcPort, tcp_header.srcPort );
    set_metadata( intrinsic_metadata.dstPort, tcp_header.dstPort );
    return select( intrinsic_metadata.payload_len ){
        0 : ingress ;
        1 : ingress ;
        2 : ingress ;
        3 : ingress ;
        default : check_src_port;
    }
}

parser parse_udp {
    extract(udp_header);
    set_metadata( intrinsic_metadata.payload_len, udp_header.length_ - 8 );
    set_metadata( intrinsic_metadata.srcPort, udp_header.srcPort );
    set_metadata( intrinsic_metadata.dstPort, udp_header.dstPort );
    return select( intrinsic_metadata.payload_len ){
        0 : ingress ;
        1 : ingress ;
        2 : ingress ;
        3 : ingress ;
        default : check_src_port;
    }
}

parser check_src_port {
    return select( intrinsic_metadata.srcPort ) {
        53 : parse_dns_header ;
        default : check_dst_port ;
    }
}

parser check_dst_port {
    return select( intrinsic_metadata.dstPort ) {
        53 : parse_dns_header ;
        default : parse_four_byte_payload ;
    }
}

parser parse_four_byte_payload {
    extract(four_byte_payload);
    return ingress;
}

parser parse_dns_header {
    extract(dns_header);
    set_metadata( intrinsic_metadata.dns_payload_len, intrinsic_metadata.payload_len - 12 );
    return parse_dns_payload;
}

parser parse_dns_payload {
    extract(one_byte_payload[next]);
    //set_metadata( dns_metadata.data, dns_metadata.data << 2 );
    //set_metadata( dns_metadata.data, one_byte_payload[dns_metadata.count].data );
    //set_metadata( dns_metadata._length, dns_metadata._length + 1 );
    return select(latest.data){
        0x00: ingress;
        default: parse_dns_payload;
    }
}

parser parse_label_header {
    extract(label_header);
    return parse_ethernet;
}

