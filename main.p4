/*
Main of Forward and Mirror
*/

#include "includes/headers.p4"
#include "includes/parsers.p4"
#include "includes/tables.p4"
#include "includes/actions.p4"

control ingress {
    apply(rule_match);
    //if( ipv4_header.protocol == IP_PROTOCOLS_TCP and label_metadata.label == 0 ) apply(classifier_tcp) ;
    //else if( ipv4_header.protocol == IP_PROTOCOLS_UDP and label_metadata.label == 0 ) apply(classifier_udp) ;
    //else if( valid(dns_header) and label_metadata.label == 0 ) apply(dns);
    //apply(forward);
    //apply(set_queue);
    apply(detect);
}

control egress {
    apply(label_encup);
}
