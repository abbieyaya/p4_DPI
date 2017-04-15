/*
Forward and Mirror
*/

/* Action */
action do_forward(out_port) {
    modify_field(standard_metadata.egress_spec, out_port);
}

action do_set_label(label, out_port) {
    modify_field(label_metadata.label, label);
    modify_field(standard_metadata.egress_spec, out_port);
}

field_list copy_to_cpu_fields {
    standard_metadata;
}

action do_copy_to_cpu(mirror_port) {
    clone_ingress_pkt_to_egress(mirror_port, copy_to_cpu_fields);
}

action do_label_encap(reason) {
    add_header(label_header);
    modify_field(label_header.label, label_metadata.label );
    modify_field(label_header.reason, reason );
}

action _drop() {
    drop();
}

action _nop() {
    no_op();
}

action do_set_priority(priority) {
    modify_field(intrinsic_metadata.priority, priority);
}

action do_assemble(){
    //modify_field(intrinsic_metadata.priority, one_byte_payload);
    pattern_match(one_byte_payload[1]);
}
