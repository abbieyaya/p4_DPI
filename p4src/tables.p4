#include "actions.p4"


table forward {
    reads {
        ipv4_base.srcAddr: exact;
    }

    actions {
        action_forward;
        _drop;
	_nop;
    }
}
