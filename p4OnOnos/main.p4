/*
Main of Forward and Mirror
*/

#include "includes/headers.p4"
#include "includes/parsers.p4"
#include "includes/tables.p4"
#include "includes/actions.p4"
#include "includes/port_counters.p4"
control ingress {
    apply(table0);
    //if( standard_metadata.egress_spec == 0 ) 
    apply(forward);
    process_port_counters();

    
    apply(rule_match);
    if( label_metadata.label == 0 and valid(quic_flags) ) {
        apply(detect_quic){
            hit {
                if( intrinsic_metadata.payload_len > ( intrinsic_metadata.quic_header_len + 4 ) )apply(set_quic);
            }
        }
    }
     
    if( label_metadata.label == 0 ) apply(detect_four_byte_payload);

    if( label_metadata.label == 0 and valid(tcp_header) ) apply(guess_by_tcp_port);
    if( label_metadata.label == 0 and valid(udp_header) ) apply(guess_by_udp_port);
    if( label_metadata.sub_label == 0 and valid(ipv4_header)) apply(guess_by_src_address);
    if( label_metadata.sub_label == 0 and valid(ipv4_header)) apply(guess_by_dst_address);
    
    if( learning_metadata._type > 0 ) apply(learning);
    
    //apply(forward);
    //apply(set_queue);

    
}

control egress {
    apply(label_encup);
}
