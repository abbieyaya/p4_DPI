/*
Forward and Mirror
*/

/* Header_type */
header_type ethernet_header_t {
    fields {
        dstAddr : 48;
        srcAddr : 48;
        etherType : 16;
    }
}

header_type ipv4_header_t {
    fields {
        version : 4;
        ihl : 4;
        diffserv : 8;
        totalLen : 16;
        identification : 16;
        flags : 3;
        fragOffset : 13;
        ttl : 8;
        protocol : 8;
        hdrChecksum : 16;
        srcAddr : 32;
        dstAddr: 32;
    }
}

header_type ipv6_header_t {
    fields {
        version : 4;
        trafficClass : 8;
        flowLabel : 20;
        payloadLen : 16;
        nextHdr : 8;
        hopLimit : 8;
        srcAddr : 128;
        dstAddr : 128;
    }
}

header_type tcp_header_t {
    fields {
        srcPort : 16;
        dstPort : 16;
        seqNo : 32;
        ackNo : 32;
        dataOffset : 4;
        res : 3;
        ecn : 3;
        ctrl : 6;
        window : 16;
        checksum : 16;
        urgentPtr : 16;
    }
}

header_type udp_header_t {
    fields {
        srcPort : 16;
        dstPort : 16;
        length_ : 16;
        checksum : 16;
    }
}

header_type label_header_t {
    fields {
        label: 8;
        reason: 8;
    }
}

header_type payload_t {
    fields {
        data : 32 ; 
    }
}


/* Metedata Type */
header_type intrinsic_metadata_t {
    fields {
        ingress_global_timestamp : 32;
        lf_field_list : 32;
        mcast_grp : 16;
        egress_rid : 16;
	    priority : 8;
        payload_len : 16;
    }
}

header_type label_metadata_t {
    fields {
        label : 8 ;
        
    }
}



/* Header */
header ethernet_header_t ethernet_header;
header ipv4_header_t ipv4_header;
header ipv6_header_t ipv6_header;
header tcp_header_t tcp_header;
header udp_header_t udp_header;
header payload_t payload_data;
header label_header_t label_header;


/* Metedata */
metadata intrinsic_metadata_t intrinsic_metadata;
metadata label_metadata_t label_metadata;


