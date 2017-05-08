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
        apply(detect_quic) {
            hit {
                if( intrinsic_metadata.payload_len > ( intrinsic_metadata.quic_header_len + 4 ) )apply(set_quic);
            }
        }
    }
    if( label_metadata.label == 0 ) apply(detect_dns);
    if( label_metadata.label == 0 ) apply(detect_whatsapp);
    if( label_metadata.label == 0 ) apply(detect_four_byte_payload);
    if( learning_metadata._type > 0 ) apply(learning);
    //apply(forward);
    //apply(set_queue);
}

control egress {
    apply(label_encup);
}
