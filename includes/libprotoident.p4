control process_libprotoident {
    // update data
    apply(calculate_index);
    apply(read_counter);
    if( five_tuple_metadata.srcPort < five_tuple_metadata.dstPort ) { 
        if( direction_metadata.counter_A == 0 ) { apply(update_A); }
        else { apply(update_counter_A); }
    }
    else {
        if( direction_metadata.counter_B == 0 ) { apply(update_B); }
        else { apply(update_counter_B); }
    }
    apply(read_all);

    /*
    // Start to detect
    if( direction_metadata.counter_A == 1 and direction_metadata.counter_B > 0 ) { apply(detect_A_two_direction); }
    else if( direction_metadata.counter_B == 1 and direction_metadata.counter_A > 0 ) { apply(detect_B_two_direction); }
    else if( direction_metadata.counter_A == 3 ){ apply(detect_A_one_direction); }
    else if( direction_metadata.counter_B == 3 ){ apply(detect_B_one_direction); }
    */
}
