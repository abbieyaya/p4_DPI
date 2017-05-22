/* Action */
#define MAX_PORTS 254
#define CPU_PORT 255
#define DROP_PORT 511

action set_egress_port(port) {
    modify_field(standard_metadata.egress_spec, port);
}

action send_to_cpu() {
    modify_field(standard_metadata.egress_spec, CPU_PORT);
}



action do_forward(out_port) {
    modify_field(standard_metadata.egress_spec, out_port);
}

field_list rule_info {
    learning_metadata._type;
    five_tuple_metadata.srcAddr;
    five_tuple_metadata.dstAddr;
    five_tuple_metadata.srcPort;
    five_tuple_metadata.dstPort;
    label_metadata.label;
    label_metadata.sub_label;
}

action do_set_label_by_guess(label) {
    modify_field(label_metadata.label, label);
    modify_field(label_metadata.label_result, 2); // by guess

    modify_field(learning_metadata._type, 1);

    //modify_field(standard_metadata.egress_spec, 2);
}

action do_set_sub_label_by_guess(sublabel) {
    modify_field(label_metadata.sub_label, sublabel);
    modify_field(label_metadata.sub_label_result, 2); // by guess

    modify_field(learning_metadata._type, 1);

    //modify_field(standard_metadata.egress_spec, 2);
}

action do_set_label_by_detect(label, sublabel) {
    
    modify_field(label_metadata.label, label);
    modify_field(label_metadata.sub_label, sublabel);
    modify_field(label_metadata.label_result, 1); // by detect
    modify_field(label_metadata.sub_label_result, 1); // by detect

    modify_field(learning_metadata._type, 1);
    
    modify_field(standard_metadata.egress_spec, 2);
    //clone_ingress_pkt_to_egress(200, copy_to_cpu_fields);
    //drop();
}

action do_set_label_by_match_rule(label, sublabel) {
    modify_field(label_metadata.label, label);
    modify_field(label_metadata.sub_label, sublabel);

    //modify_field(standard_metadata.egress_spec, 3);
    //clone_ingress_pkt_to_egress(300, copy_to_cpu_fields);
    //drop();
}

action do_label_encap() {
    add_header(label_header);
    modify_field(label_header.label, label_metadata.label );
    modify_field(label_header.sub_label, label_metadata.sub_label );
    modify_field(label_header.label_result, label_metadata.label_result );
    modify_field(label_header.sub_label_result, label_metadata.sub_label_result );
}

action _drop() {
    drop();
}

action _nop() {
    no_op();
}

action do_assemble(){
    modify_field(label_metadata.label, 2);
    //pattern_match(label_metadata.sub_label, one_byte_payload);
    modify_field(label_metadata.label_result, 1); // by detect
    modify_field(label_metadata.sub_label_result, 1); // by detect
    
    //modify_field(standard_metadata.egress_spec, 2);

    modify_field(learning_metadata._type, 1);
    
    //clone_ingress_pkt_to_egress(200, copy_to_cpu_fields);
    //drop();
}


action do_set_priority(priority) {
    modify_field(intrinsic_metadata.priority, priority);
}

field_list copy_to_cpu_fields {
    standard_metadata;
    label_metadata;
}

action do_learning() {
    generate_digest( 1, rule_info );
    clone_ingress_pkt_to_egress( 2, copy_to_cpu_fields );
}



field_list hash_fields {
    five_tuple_metadata.srcAddr;
    five_tuple_metadata.dstAddr;
    five_tuple_metadata.srcPort;
    five_tuple_metadata.dstPort;
    five_tuple_metadata.protocol;
}

field_list_calculation index_hash_cal {
    input { 
        hash_fields;
    }
    algorithm : crc16;
    output_width : 16;
}

register dirA_payload_1{
    width : 32;
    instance_count : 1024;
}

register dirA_payload_2{
    width : 32;
    instance_count : 1024;
}

register dirA_payload_3{
    width : 32;
    instance_count : 1024;
}

register dirB_payload_1{
    width : 32;
    instance_count : 1024;
}

register dirB_payload_2{
    width : 32;
    instance_count : 1024;
}

register dirB_payload_3{
    width : 32;
    instance_count : 1024;
}

register dirA_length_1{
    width : 16;
    instance_count : 1024;
}

register dirA_length_2{
    width : 16;
    instance_count : 1024;
}

register dirA_length_3{
    width : 16;
    instance_count : 1024;
}

register dirB_length_1{
    width : 16;
    instance_count : 1024;
}

register dirB_length_2{
    width : 16;
    instance_count : 1024;
}

register dirB_length_3{
    width : 16;
    instance_count : 1024;
}

register dirA_counter{
    width : 16;
    instance_count : 1024;
}

register dirB_counter{
    width : 16;
    instance_count : 1024;
}

action do_calculate_index(){
    modify_field_with_hash_based_offset(direction_metadata.hash_index, 0, index_hash_cal, 1024);
}

action do_read_all(){
    register_read(direction_metadata.payload_A1 , dirA_payload_1, direction_metadata.hash_index);
    register_read(direction_metadata.payload_A2 , dirA_payload_2, direction_metadata.hash_index);
    register_read(direction_metadata.payload_A3 , dirA_payload_3, direction_metadata.hash_index);
    register_read(direction_metadata.payload_B1 , dirB_payload_1, direction_metadata.hash_index);
    register_read(direction_metadata.payload_B2 , dirB_payload_2, direction_metadata.hash_index);
    register_read(direction_metadata.payload_B3 , dirB_payload_3, direction_metadata.hash_index);

    register_read(direction_metadata.length_A1 , dirA_length_1, direction_metadata.hash_index);
    register_read(direction_metadata.length_A2 , dirA_length_2, direction_metadata.hash_index);
    register_read(direction_metadata.length_A3 , dirA_length_3, direction_metadata.hash_index);
    register_read(direction_metadata.length_B1 , dirB_length_1, direction_metadata.hash_index);
    register_read(direction_metadata.length_B2 , dirB_length_2, direction_metadata.hash_index);
    register_read(direction_metadata.length_B3 , dirB_length_3, direction_metadata.hash_index);
}

action do_read_counter(){
    register_read(direction_metadata.counter_A , dirA_counter, direction_metadata.hash_index);
    register_read(direction_metadata.counter_B , dirA_counter, direction_metadata.hash_index);

}

action do_updateA_1(){
    register_write( dirA_payload_1, direction_metadata.hash_index, four_byte_payload.data );
    register_write( dirA_length_1, direction_metadata.hash_index, intrinsic_metadata.payload_len );
    register_write( dirA_counter, direction_metadata.hash_index, 1 );
    
}

action do_updateA_2(){
    register_write( dirA_payload_2, direction_metadata.hash_index, four_byte_payload.data );
    register_write( dirA_length_2, direction_metadata.hash_index, intrinsic_metadata.payload_len );
    register_write( dirA_counter, direction_metadata.hash_index, 2 );
}

action do_updateA_3(){
    register_write( dirA_payload_3, direction_metadata.hash_index, four_byte_payload.data );
    register_write( dirA_length_3, direction_metadata.hash_index, intrinsic_metadata.payload_len );
    register_write( dirA_counter, direction_metadata.hash_index, 3 );
}

action do_updateB_1(){
    register_write( dirB_payload_1, direction_metadata.hash_index, four_byte_payload.data );
    register_write( dirB_length_1, direction_metadata.hash_index, intrinsic_metadata.payload_len );
    register_write( dirB_counter, direction_metadata.hash_index, 1 );
}

action do_updateB_2(){
    register_write( dirB_payload_2, direction_metadata.hash_index, four_byte_payload.data );
    register_write( dirB_length_2, direction_metadata.hash_index, intrinsic_metadata.payload_len );
    register_write( dirB_counter, direction_metadata.hash_index, 2 );
}

action do_updateB_3(){
    register_write( dirB_payload_3, direction_metadata.hash_index, four_byte_payload.data );
    register_write( dirB_length_3, direction_metadata.hash_index, intrinsic_metadata.payload_len );
    register_write( dirB_counter, direction_metadata.hash_index, 3 );
}

