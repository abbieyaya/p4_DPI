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

    
    // Start to detect
    
    if( ( direction_metadata.counter_A == 1 and direction_metadata.counter_B > 0 ) or 
        ( direction_metadata.counter_B == 1 and direction_metadata.counter_A > 0 ) ) { 
            apply(detect_two_direction); 
            apply(reset_direction); 
    }
    else if( direction_metadata.counter_A == 2 or direction_metadata.counter_B == 2 ) {
        if( direction_metadata.counter_A == 2 ){ apply(copy_A_fields); }
        else { apply(copy_B_fields); }
        apply(detect_one_direction); 
        apply(reset_direction); 
    }
}
