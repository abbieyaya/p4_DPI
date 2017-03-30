/*
Main of Forward and Mirror
*/

#include "includes/headers.p4"
#include "includes/parsers.p4"
#include "includes/tables.p4"
#include "includes/actions.p4"

control ingress {
    //apply(copy_to_cpu);
    //if( ipv4_header.protocol == IP_PROTOCOLS_TCP ) apply(classifier_tcp) ;
    //else if( ipv4_header.protocol == IP_PROTOCOLS_UDP ) apply(classifier_udp) ;
    //apply(forward);
    //apply(set_queue);
    apply(detect);
}

control egress {
    apply(label_encup);
}
