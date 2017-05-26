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
    set_metadata( five_tuple_metadata.srcAddr, ipv4_header.srcAddr );
    set_metadata( five_tuple_metadata.dstAddr, ipv4_header.dstAddr );
    set_metadata( five_tuple_metadata.protocol, ipv4_header.protocol );
    return select(latest.protocol) {
        IP_PROTOCOLS_TCP : parse_tcp;
        IP_PROTOCOLS_UDP : parse_udp;
        default : ingress;
    }
}

parser parse_ipv6 {
    extract(ipv6_header);
    set_metadata( five_tuple_metadata.srcAddr, ipv6_header.srcAddr );
    set_metadata( five_tuple_metadata.dstAddr, ipv6_header.dstAddr );
    set_metadata( five_tuple_metadata.protocol, ipv6_header.nextHdr );
    return select(latest.nextHdr) {
        IP_PROTOCOLS_TCP : parse_tcp;
        IP_PROTOCOLS_UDP : parse_udp;
        default : ingress;
    }
}

parser parse_tcp {
    extract(tcp_header);
    set_metadata( intrinsic_metadata.payload_len, ipv4_header.totalLen - 20 /*IP_header*/ - ( tcp_header.dataOffset * 4 ) /*TCP_header*/  );
    set_metadata( intrinsic_metadata.tcp_hdr_len, tcp_header.dataOffset * 4 );
    set_metadata( five_tuple_metadata.srcPort, tcp_header.srcPort );
    set_metadata( five_tuple_metadata.dstPort, tcp_header.dstPort );
    return select( intrinsic_metadata.payload_len ){
        0 : ingress ;
        1 : ingress ;
        2 : ingress ;
        3 : ingress ;
        default : check_tcp_port;
    }
}

parser parse_udp {
    extract(udp_header);
    set_metadata( intrinsic_metadata.payload_len, udp_header.length_ - 8 );
    set_metadata( five_tuple_metadata.srcPort, udp_header.srcPort );
    set_metadata( five_tuple_metadata.dstPort, udp_header.dstPort );
    return select( intrinsic_metadata.payload_len ){
        0 : ingress ;
        1 : ingress ;
        2 : ingress ;
        3 : ingress ;
        default : check_udp_port;
    }
}

parser check_tcp_port {
    return select( five_tuple_metadata.srcPort, five_tuple_metadata.dstPort ) {
        0x00000035 mask 0x0000ffff : parse_dns_header ; // port = 53
        0x00350000 mask 0xffff0000 : parse_dns_header ;
        0x000001BB mask 0x0000ffff : parse_tls_header ; // port = 443
        0x01BB0000 mask 0xffff0000 : parse_tls_header ;
        
        default : parse_byte_payload ;
    }
}

parser check_udp_port {
    return select( five_tuple_metadata.srcPort, five_tuple_metadata.dstPort ) {
        0x00000035 mask 0x0000ffff : parse_dns_header ;  // port = 53
        0x00350000 mask 0xffff0000 : parse_dns_header ;  
        0x000001BB mask 0x0000ffff : parse_quic_flags ;  // port = 443
        0x01BB0000 mask 0xffff0000 : parse_quic_flags ;  
        0x00000050 mask 0x0000ffff : parse_quic_flags ;  // port = 80
        0x00500000 mask 0xffff0000 : parse_quic_flags ;
        default : parse_byte_payload ;
    }
}

parser parse_byte_payload {
    return select(current(0,16)){
        0x5741 : parse_whatsapp_three_byte_payload ;
        default : parse_four_byte_payload ;
    }
}

parser parse_whatsapp_three_byte_payload {
    extract(one_byte_payload[next]);
    extract(one_byte_payload[next]);
    extract(whatsapp_three_byte_payload);
    return ingress;
}

parser parse_four_byte_payload {
    extract(four_byte_payload);
    return ingress;
}

parser parse_dns_header {
    extract(dns_header);
    //set_metadata( intrinsic_metadata.dns_payload_len, intrinsic_metadata.payload_len - 12 );
    return parse_dns_payload;
}

parser parse_dns_payload {
    extract(one_byte_payload[next]);
    return select(current(0, 8)){
        0x00: ingress;
        default: parse_dns_payload;
    }
}

parser parse_label_header {
    extract(label_header);
    return parse_ethernet;
}

parser parse_quic_flags {
    extract(quic_flags);
    set_metadata( intrinsic_metadata.version_len, quic_flags.version << 2 );
    set_metadata( intrinsic_metadata.cid_len, 1 << quic_flags.cid_len );
    set_metadata( intrinsic_metadata.seq_len, 1 << quic_flags.seq_len );
    set_metadata( intrinsic_metadata.quic_header_len, 1 /*flag len*/ + intrinsic_metadata.version_len + intrinsic_metadata.cid_len + intrinsic_metadata.seq_len );
    return ingress ;
}

parser parse_tls_header {
    extract(tls_records_header);
    return select(tls_records_header._type){
        0x16 : parse_handshake_protocol;
        default : ingress;
    }
}

parser parse_handshake_protocol {
    extract(handshake_protocol);
    return select(tls_records_header._type){
        0x01 : parse_client_hello;
        default : ingress;
    }
}

parser parse_client_hello {
    extract(client_hello);
    extract(cipher_suites);
    extract(compression_methods);
    return parse_extension;
}

parser parse_extension{
    return select(current(0,16)){
        0x0000 : parse_server_name_indication ;
        default : parse_extension;
    }
}

parser parse_server_name_indication {
    extract(server_name_indication);
    return parse_host_name;
}

parser parse_host_name {
    extract(one_byte_payload[next]);
    set_metadata( intrinsic_metadata.version_len, quic_flags.version << 2 ); 
    return select(intrinsic_metadata.ssl_host_name){
        0x0000 : ingress;
        default : parse_host_name ;
    }
}
