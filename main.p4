/*
Main of Forward and Mirror
*/

#include "includes/headers.p4"
#include "includes/parsers.p4"
#include "includes/tables.p4"
#include "includes/actions.p4"

control ingress {
    apply(rule_match);
    if( label_metadata.label == 0 ) {
        apply(quic) {
            hit {
                if( intrinsic_metadata.payload_len > ( intrinsic_metadata.quic_header_len + 4 ) )apply(set_quic);
            }
        }
    }
    //if( ipv4_header.protocol == IP_PROTOCOLS_TCP and label_metadata.label == 0 ) apply(classifier_tcp) ;
    //else if( ipv4_header.protocol == IP_PROTOCOLS_UDP and label_metadata.label == 0 ) apply(classifier_udp) ;
    if( label_metadata.label == 0 ) apply(dns);
    if( label_metadata.label == 0 ) apply(detect);
    //apply(forward);
    //apply(set_queue);
}

control egress {
    apply(label_encup);
}
