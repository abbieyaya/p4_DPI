/*
   Forward and Mirror
 */

/* Table */

table table0 {
    reads {
        standard_metadata.ingress_port : ternary;
        ethernet_header.dstAddr : ternary;
        ethernet_header.srcAddr : ternary;
        ethernet_header.etherType : ternary;
    }
    actions {
        set_egress_port;
        send_to_cpu;
        _drop;
    }
    support_timeout: true;
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

table host_to_physical {
    reads {
        ethernet_header.srcAddr : exact ;
    }

    actions {
        do_forward;
    }
}

table physical_to_host {
    reads {
        ethernet_header.dstAddr : exact ;
    }

    actions {
        do_forward;
    }
}

table label_encup {
    reads { 
        //standard_metadata.instance_type : exact ;
        learning_metadata._type : exact ;
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
    actions {
        do_assemble ;
    }
}

table detect_quic {
    reads {
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

table guess_by_tcp_port {
    reads {
        tcp_header.srcPort : ternary;
        tcp_header.dstPort : ternary;
    }

    actions {
        do_set_label_by_guess ;
    }
}

table guess_by_udp_port {
    reads {
        udp_header.srcPort : ternary;
        udp_header.dstPort : ternary;
    }

    actions {
        do_set_label_by_guess ;
    }
}

table guess_by_src_address {
    reads {
        ipv4_header.srcAddr: lpm;
    }

    actions {
        do_set_sub_label_by_guess ;
    }
}

table guess_by_dst_address {
    reads {
        ipv4_header.dstAddr: lpm;
    }

    actions {
        do_set_sub_label_by_guess ;
    }
}

table learning {
    reads {
        learning_metadata._type : range ;
    }

    actions {
        do_learning ;
    }
}
