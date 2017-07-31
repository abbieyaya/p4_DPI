/*
   Main of Forward and Mirror
 */

#include "includes/headers.p4"
#include "includes/parsers.p4"
#include "includes/tables.p4"
#include "includes/actions.p4"
#include "includes/port_counters.p4"
#include "includes/libprotoident.p4"
control ingress {
    
    if( ( valid(ipv4_header) or valid(ipv6_header) ) and intrinsic_metadata.payload_len > 0 ) {
        apply(table0);
        //process_port_counters();

        apply(rule_match);
        if( label_metadata.label == 0 and valid(quic_flags) ) {
            apply(detect_quic){
                hit {
                    if( intrinsic_metadata.payload_len > ( intrinsic_metadata.quic_header_len + 4 ) )apply(set_quic);
                }
            }
        }

        if( label_metadata.label == 0 and valid(server_name_indication) ) apply(detect_ssl);
        if( label_metadata.label == 0 and valid(dns_header) ) apply(detect_dns);
        if( label_metadata.label == 0 and valid(whatsapp_three_byte_payload) ) apply(detect_whatsapp);
        if( label_metadata.label == 0 ) apply(detect_four_byte_payload);

        //if( learning_metadata._type == 0 ) process_libprotoident();
    }
    
    if ( learning_metadata._type == 0 or label_metadata.label == 0x3 ) {
        if( label_metadata.label == 0 and valid(tcp_header) ) apply(guess_by_tcp_port);
        if( label_metadata.label == 0 and valid(udp_header) ) apply(guess_by_udp_port);
        if( label_metadata.sub_label == 0 and valid(ipv4_header)) apply(guess_by_src_address);
        if( label_metadata.sub_label == 0 and valid(ipv4_header)) apply(guess_by_dst_address);
    }

    //process_libprotoident();
    if( learning_metadata._type > 0 ) apply(learning);
    
    apply(forward);
    //apply(set_queue);
}

control egress {
    apply(label_encup);
}
