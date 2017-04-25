/* Copyright 2013-present Barefoot Networks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Antonin Bas (antonin@barefootnetworks.com)
 *
 */

#include <bm/bm_apps/learn.h>
#include <bm/Standard.h>
#include <bm/SimplePre.h>

#include <iostream>
#include <string>
#include <vector>
#include <string>
#include <cassert>
#include <string.h>
using namespace std;
namespace {

    bm_apps::LearnListener *listener;

    struct rule_t {
        uint8_t type;
        char src_addr[16];
        char dst_addr[16];
        char src_port[2];
        char dst_port[2];
        char label;
        char sub_label;
    } __attribute__((packed));

}  // namespace

namespace runtime = bm_runtime::standard;

namespace {

    void learn_cb(const bm_apps::LearnListener::MsgInfo &msg_info, const char *data, void *cookie) {
        (void) cookie;

        boost::shared_ptr<runtime::StandardClient> client = listener->get_client();
        assert(client);

        int length = 0 ;
        for (unsigned int i = 0; i < msg_info.num_samples; i++) {
            char *dup = strdup( data );
            dup[5] = '\0' ;
            const rule_t *temp = (const rule_t*)(data + length)  ;
            printf( "type:%d\n", temp->type );
            auto add_entry = [client](
                    const std::string &t_name, const runtime::BmMatchParams &match_params,
                    const std::string &a_name, const runtime::BmActionData &action_data) {
                runtime::BmAddEntryOptions options;
                try {
                    runtime::BmEntryHandle entryHandle ;
                    entryHandle = client->bm_mt_add_entry(0, t_name, match_params, a_name, action_data, options);
                    //client->bm_mt_set_entry_ttl(0, t_name, entryHandle, 10000) ;
                    cout << entryHandle << "\n" ;
                } catch (runtime::InvalidTableOperation &ito) {
                    auto what = runtime::_TableOperationErrorCode_VALUES_TO_NAMES.find(ito.code)->second;
                    std::cout << "Invalid table (" << t_name << ") operation (" << ito.code << "): " << what << std::endl;
                }
            };
            if( temp->type == 1 ) {
                const rule_t *rule = (const rule_t*)(data + length)  ;
                printf( "IN (%x%x<->%x%x)\n", rule->src_port[0], rule->src_port[1]
                                        , rule->dst_port[0], rule->dst_port[1] ) ;
                
                // set key 
                runtime::BmMatchParam match_param_src_addr;
                runtime::BmMatchParam match_param_dst_addr;
                runtime::BmMatchParam match_param_src_port;
                runtime::BmMatchParam match_param_dst_port;

                match_param_src_addr.type = runtime::BmMatchParamType::type::EXACT;
                match_param_dst_addr.type = runtime::BmMatchParamType::type::EXACT;
                match_param_src_port.type = runtime::BmMatchParamType::type::EXACT;
                match_param_dst_port.type = runtime::BmMatchParamType::type::EXACT;

                runtime::BmMatchParamExact match_param_exact;

                match_param_exact.key = std::string(rule->src_addr, 16);
                match_param_src_addr.__set_exact(match_param_exact);
                match_param_exact.key = std::string(rule->dst_addr, 16);
                match_param_dst_addr.__set_exact(match_param_exact);
                match_param_exact.key = std::string(rule->src_port, 2) ;
                //match_param_exact.key = std::string(reinterpret_cast<const char *>(&rule->src_port));
                match_param_src_port.__set_exact(match_param_exact);
                match_param_exact.key = std::string(rule->dst_port, 2);
                //match_param_exact.key = std::string(reinterpret_cast<const char *>(&rule->dst_port));
                match_param_dst_port.__set_exact(match_param_exact);


                runtime::BmMatchParams match_params_direction_A({match_param_src_addr, match_param_dst_addr,
                                                                match_param_src_port, match_param_dst_port});
                runtime::BmMatchParams match_params_direction_B({match_param_dst_addr, match_param_src_addr,
                                                                match_param_dst_port, match_param_src_port});
                // set action value 
                std::vector<std::string> action_datas = {std::string(1 , rule->label ),std::string(1 , rule->sub_label )};

                add_entry("rule_match", match_params_direction_A, "do_set_label_by_match_rule", action_datas);
                add_entry("rule_match", match_params_direction_B, "do_set_label_by_match_rule", action_datas);
            }
            length += sizeof(rule_t) ;
            //cout << "len:"<< length << "\n";
        }

        client->bm_learning_ack_buffer(0, msg_info.list_id, msg_info.buffer_id);
    }

}  // namespace

int main() {
    listener = new bm_apps::LearnListener("ipc:///tmp/bmv2-0-notifications.ipc");
    listener->register_cb(learn_cb, nullptr);
    listener->start();

    while (true) std::this_thread::sleep_for(std::chrono::seconds(100));

    return 0;
}
