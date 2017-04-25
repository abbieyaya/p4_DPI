/*
   Forward and Mirror
 */

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
        do_forward;
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
        do_forward;
    }
}

table forward {
    reads {
        ipv4_header.dstAddr: exact;
    }

    actions { 
        do_forward;
        _nop;
    }
}

table label_encup {
    reads { 
        label_metadata.label : exact; 
    }
    actions { 
        _drop; 
        do_label_encap; 
    }
    size : 16;
}

table set_queue {
    reads {
        ipv4_header.srcAddr: exact;
        ipv4_header.dstAddr: exact;
    }

    actions {
        do_set_priority ;
    }
} 

table detect {
    reads {
        four_byte_payload.data : ternary ;
    }

    actions {
        do_set_label_by_detect ;
    }

}

table dns {
    reads {
        dns_header : valid;
        //one_byte_payload : exact;
    }

    actions {
        do_assemble ;
    }
}

table rule_match {
    reads {
        five_tuple_metadata.srcAddr: exact;
        five_tuple_metadata.dstAddr: exact;
        five_tuple_metadata.srcPort: exact;
        five_tuple_metadata.dstPort: exact;
    }

    actions {
        do_set_label_by_match_rule;
    }
}
