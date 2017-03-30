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
    actions { _drop; do_label_encap; }
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
        payload_data.data : ternary ;
    }

    actions {
        do_set_label ;
    }

}

