#include "parsers.p4"


action action_forward(out_port) {
    modify_field(standard_metadata.egress_spec, out_port);
}

action _drop() {
    drop();
}

action _nop() {
}
