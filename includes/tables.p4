/*
   Forward and Mirror
 */

/* Table */
table copy_to_cpu {
    actions {do_copy_to_cpu;}
    // size : 1;
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

table detect_four_byte_payload {
    reads {
        four_byte_payload.data : ternary ;
        intrinsic_metadata.payload_len : ternary ;
    }

    actions {
        do_set_label_by_detect ;
    }

}

table detect_dns {
    reads {
        dns_header : valid;
        //one_byte_payload : exact;
    }

    actions {
        do_assemble ;
    }
}

table detect_quic {
    reads {
        quic_flags : valid;
        quic_flags.reset : exact;
        quic_flags.reserved: exact ;
    }
    actions {
        _nop ;
    }
}

table set_quic {
    actions {
        do_set_label_by_detect ;
    }
}

table detect_whatsapp {
    reads {
        whatsapp_three_byte_payload : valid;
        whatsapp_three_byte_payload.payload_1 : range;
        whatsapp_three_byte_payload.payload_2 : range;
        whatsapp_three_byte_payload.payload_3 : exact;
    }

    actions { 
        do_set_label_by_detect ;
    }
}
