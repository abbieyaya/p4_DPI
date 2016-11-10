#include "headers.p4"

parser start {
    return parse_ethernet;
}

parser parse_ethernet {
    extract(ethernet);
    return select(ethernet.etherType) {
        0x0800 : parse_ipv4;
        default: ingress;   
    }
}

parser parse_ipv4 {
    extract(ipv4_base);
    return ingress;
}
