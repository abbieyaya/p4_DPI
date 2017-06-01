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

#include <bm/bm_sim/actions.h>
#include <bm/bm_sim/calculations.h>
#include <bm/bm_sim/core/primitives.h>
#include <bm/bm_sim/counters.h>
#include <bm/bm_sim/meters.h>
#include <bm/bm_sim/packet.h>
#include <bm/bm_sim/phv.h>
#include <random>
#include <thread>

#include <bm/bm_sim/logger.h>
template <typename... Args>
using ActionPrimitive = bm::ActionPrimitive<Args...>;

using bm::Data;
using bm::Field;
using bm::Header;
using bm::MeterArray;
using bm::CounterArray;
using bm::RegisterArray;
using bm::NamedCalculation;
using bm::HeaderStack;

class pattern_match : public ActionPrimitive<Field &, HeaderStack &> {
  bool match(char text[], std::string temp){
    char* pattern = strdup(temp.c_str());
    int text_length = strlen(text) ;
    int pattern_length = strlen(pattern);
    int c, e, d ;

    if ( pattern_length > text_length ){
        free(pattern);
        return false ;
    }else {
        BMLOG_DEBUG("~~~~~~~~~~~~~~~~~~~~~~~~~~~compare {} {}\n", text, temp );
        for (c = 0; c <= text_length - pattern_length; c++) {
            e = c;
             
            for (d = 0; d < pattern_length; d++) {
                if (pattern[d] == text[e]) {
                    e++;
                }
                else {
                    break;
                }
            }
            if (d == pattern_length) {
                free(pattern);
                return true ;
                break ;
            }
        }
    }

    free(pattern);
    return false ; 
  }


  void operator ()(Field & result, HeaderStack &data) {
    char text[100] ;
    memset(text, '\0', 100);
    int now = 0 ;
    for (size_t i = 0; i < data.get_count(); i++, now++ ) {
        auto &hdr_instance = data.at(i);
        assert(hdr_instance.is_valid());
        //auto &bc = hdr_instance.get_bytes();
        //assert(bc.size() == 1);
        // hdr_instance.get_field(0).set_arith(1);
        //text[i] = hdr_instance.get_field(0).get_uint() ;
        text[now] = (hdr_instance.get_field(0).get_bytes())[0] ;
        if ( text[now] == 0 ) {
            break ;
        }
        if ( now == 0 && text[now] < 32 ) {
            now-- ;
        } else {
            text[now] = text[now] < 32 ? '.' : text[now] ;
        }
    }
 
    //BMLOG_DEBUG("~~~~~~~~~~~~~~~~~~~~~~~~~~~find {} \n", text );
    if ( match( text, ".skype." ) || match( text, ".skypeassets." ) || match( text, ".skypedata." )
             || match( text, ".skypeecs-" ) || match( text, ".skypeforbusiness." ) || match( text, ".lync.com" ) 
             || match( text, "e7768.b.akamaiedge.net" ) || match( text, "e4593.dspg.akamaiedge.net" ) || match( text, "e4593.g.akamaiedge.net" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find SKYPE\n");
        result.set(1);
    }else if ( match( text, ".yahoo." ) || match( text, ".yimg.com" ) || match( text, "yahooapis." ) ) {
        BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find yahoo\n");
        result.set(2);
    }else if ( match( text, "wikipedia." ) || match( text, "wikimedia." ) 
             || match( text, "mediawiki." ) || match( text, "wikimediafoundation." ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(3);
    }else if ( match( text, ".whatsapp." ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(4);
    }else if ( match( text, "torrent." ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(5);
    }else if ( match( text, "maps.google." ) || match( text, "maps.gstatic.com" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(6);
    }else if ( match( text, ".gmail." ) || match( text, "mail.google." ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(7);
    }else if ( match( text, "facebook.com" ) || match( text, "fbstatic-a.akamaihd.net" ) || match( text, ".fbcdn.net" )
             || match( text, "fbcdn-" ) || match( text, ".facebook.net" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(8);
    }else if ( match( text, ".twttr.com" ) || match( text, "twitter." ) || match( text, "twimg.com" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(9);
    }else if ( match( text, ".wechat.com" ) || match( text, ".wechat.org" ) || match( text, ".wechatapp.com" )
             || match( text, ".we.chat" ) || match( text, ".wx." ) || match( text, ".weixin." ) 
            || match( text, ".mmsns.qpic.cn" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(10);
    }else if ( match( text, "netflix.com" ) || match( text, "nflxext.com" ) || match( text, "nflximg.com" )
             || match( text, "nflximg.net" ) || match( text, "nflxvideo.net" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(11);
    }else if ( match( text, ".apple.com" ) || match( text, ".mzstatic.com" ) 
             || match( text, ".icloud.com" ) || match( text, "itunes.apple.com" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(12);
    }else if ( match( text, "youtube." ) || match( text, "yt3.ggpht.com" ) || match( text, ".googlevideo.com" ) 
             || match( text, ".ytimg.com" ) || match( text, "youtube-nocookie.") ){ 
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(21);
    }else if ( match( text, ".google." ) || match( text, ".gstatic.com" ) || match( text, ".googlesyndication.com" )
             || match( text, ".googletagservices.com" ) || match( text, ".2mdn.net" ) || match( text, ".doubleclick.net" ) 
             || match( text, "googleads." ) || match( text, "google-analytics." ) || match( text, "googleusercontent." ) 
             || match( text, "googleadservices." ) || match( text, "googleapis.com" ) || match( text, "ggpht.com" ) 
             || match( text, "1e100.net" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(13);
    }else if ( match( text, ".dropbox.com" ) || match( text, "log.getdropbox.com" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(14);
    }else if ( match( text, "ttvnw.net" ) || match( text, "static-cdn.jtvnw.net" ) || match( text, "www-cdn.jtvnw.net"  ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(15);
    }else if ( match( text, "github.com" ) || match( text, "github.io" ) || match( text, "githubusercontent.com" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(16);
    }else if ( match( text, ".steampowered.com" ) || match( text, "steamcommunity.com" ) || match( text, ".steamcontent.com" )
             || match( text, ".steamstatic.com" ) || match( text, "steamcommunity-a.akamaihd.net" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(17);
    }else if ( match( text, ".ppstream.com" ) || match( text, ".pps.tv" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(18);
    }else if ( match( text, ".cdninstagram.com" ) || match( text, "instagram." ) || match( text, ".instagram." )
             || match( text, "igcdn-photos-" ) || match( text, "instagramimages-" ) || match( text, "instagramstatic-" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(19);
    }else if ( match( text, ".cnn.c" ) || match( text, ".cnn.net" ) ) {
        //BMLOG_DEBUG("YES!!!!!!!!!!!!!!!!!!! I find google\n");
        result.set(20);
    }else {
        //BMLOG_DEBUG("NO QQQQQQQQQQQQQQQQQQQQQ \n");
        result.set(0);
    }
  }
};

REGISTER_PRIMITIVE(pattern_match);

class modify_field : public ActionPrimitive<Data &, const Data &> {
  void operator ()(Data &dst, const Data &src) {
    bm::core::assign()(dst, src);
  }
};

REGISTER_PRIMITIVE(modify_field);

class modify_field_rng_uniform
  : public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &b, const Data &e) {
    // TODO(antonin): a little hacky, fix later if there is a need using GMP
    // random fns
    using engine = std::default_random_engine;
    using hash = std::hash<std::thread::id>;
    static thread_local engine generator(hash()(std::this_thread::get_id()));
    using distrib64 = std::uniform_int_distribution<uint64_t>;
    distrib64 distribution(b.get_uint64(), e.get_uint64());
    f.set(distribution(generator));
  }
};

REGISTER_PRIMITIVE(modify_field_rng_uniform);

class add_to_field : public ActionPrimitive<Field &, const Data &> {
  void operator ()(Field &f, const Data &d) {
    f.add(f, d);
  }
};

REGISTER_PRIMITIVE(add_to_field);

class subtract_from_field : public ActionPrimitive<Field &, const Data &> {
  void operator ()(Field &f, const Data &d) {
    f.sub(f, d);
  }
};

REGISTER_PRIMITIVE(subtract_from_field);

class add : public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.add(d1, d2);
  }
};

REGISTER_PRIMITIVE(add);

class subtract : public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.sub(d1, d2);
  }
};

REGISTER_PRIMITIVE(subtract);

class bit_xor : public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.bit_xor(d1, d2);
  }
};

REGISTER_PRIMITIVE(bit_xor);

class bit_or : public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.bit_or(d1, d2);
  }
};

REGISTER_PRIMITIVE(bit_or);

class bit_and : public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.bit_and(d1, d2);
  }
};

REGISTER_PRIMITIVE(bit_and);

class shift_left :
  public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.shift_left(d1, d2);
  }
};

REGISTER_PRIMITIVE(shift_left);

class shift_right :
  public ActionPrimitive<Data &, const Data &, const Data &> {
  void operator ()(Data &f, const Data &d1, const Data &d2) {
    f.shift_right(d1, d2);
  }
};

REGISTER_PRIMITIVE(shift_right);

class drop : public ActionPrimitive<> {
  void operator ()() {
    get_field("standard_metadata.egress_spec").set(511);
    if (get_phv().has_field("intrinsic_metadata.mcast_grp")) {
      get_field("intrinsic_metadata.mcast_grp").set(0);
    }
  }
};

REGISTER_PRIMITIVE(drop);

class exit_ : public ActionPrimitive<> {
  void operator ()() {
    get_packet().mark_for_exit();
  }
};

REGISTER_PRIMITIVE_W_NAME("exit", exit_);

class generate_digest : public ActionPrimitive<const Data &, const Data &> {
  void operator ()(const Data &receiver, const Data &learn_id) {
    // discared receiver for now
    (void) receiver;
    get_field("intrinsic_metadata.lf_field_list").set(learn_id);
  }
};

REGISTER_PRIMITIVE(generate_digest);

class add_header : public ActionPrimitive<Header &> {
  void operator ()(Header &hdr) {
    // TODO(antonin): reset header to 0?
    if (!hdr.is_valid()) {
      hdr.reset();
      hdr.mark_valid();
      // updated the length packet register (register 0)
      auto &packet = get_packet();
      packet.set_register(0, packet.get_register(0) + hdr.get_nbytes_packet());
    }
  }
};

REGISTER_PRIMITIVE(add_header);

class add_header_fast : public ActionPrimitive<Header &> {
  void operator ()(Header &hdr) {
    hdr.mark_valid();
  }
};

REGISTER_PRIMITIVE(add_header_fast);

class remove_header : public ActionPrimitive<Header &> {
  void operator ()(Header &hdr) {
    if (hdr.is_valid()) {
      // updated the length packet register (register 0)
      auto &packet = get_packet();
      packet.set_register(0, packet.get_register(0) - hdr.get_nbytes_packet());
      hdr.mark_invalid();
    }
  }
};

REGISTER_PRIMITIVE(remove_header);

class copy_header : public ActionPrimitive<Header &, const Header &> {
  void operator ()(Header &dst, const Header &src) {
    bm::core::assign_header()(dst, src);
  }
};

REGISTER_PRIMITIVE(copy_header);

/* standard_metadata.clone_spec will contain the mirror id (16 LSB) and the
   field list id to copy (16 MSB) */
class clone_ingress_pkt_to_egress
  : public ActionPrimitive<const Data &, const Data &> {
  void operator ()(const Data &clone_spec, const Data &field_list_id) {
    Field &f_clone_spec = get_field("standard_metadata.clone_spec");
    f_clone_spec.shift_left(field_list_id, 16);
    f_clone_spec.add(f_clone_spec, clone_spec);
  }
};

REGISTER_PRIMITIVE(clone_ingress_pkt_to_egress);

class clone_egress_pkt_to_egress
  : public ActionPrimitive<const Data &, const Data &> {
  void operator ()(const Data &clone_spec, const Data &field_list_id) {
    Field &f_clone_spec = get_field("standard_metadata.clone_spec");
    f_clone_spec.shift_left(field_list_id, 16);
    f_clone_spec.add(f_clone_spec, clone_spec);
  }
};

REGISTER_PRIMITIVE(clone_egress_pkt_to_egress);

class resubmit : public ActionPrimitive<const Data &> {
  void operator ()(const Data &field_list_id) {
    if (get_phv().has_field("intrinsic_metadata.resubmit_flag")) {
      get_phv().get_field("intrinsic_metadata.resubmit_flag")
          .set(field_list_id);
    }
  }
};

REGISTER_PRIMITIVE(resubmit);

class recirculate : public ActionPrimitive<const Data &> {
  void operator ()(const Data &field_list_id) {
    if (get_phv().has_field("intrinsic_metadata.recirculate_flag")) {
      get_phv().get_field("intrinsic_metadata.recirculate_flag")
          .set(field_list_id);
    }
  }
};

REGISTER_PRIMITIVE(recirculate);

class modify_field_with_hash_based_offset
  : public ActionPrimitive<Data &, const Data &,
                           const NamedCalculation &, const Data &> {
  void operator ()(Data &dst, const Data &base,
                   const NamedCalculation &hash, const Data &size) {
    uint64_t v =
      (hash.output(get_packet()) % size.get<uint64_t>()) + base.get<uint64_t>();
    dst.set(v);
    BMLOG_DEBUG("~~~~~~~ hash result: {}\n", v );
  }
};

REGISTER_PRIMITIVE(modify_field_with_hash_based_offset);

class no_op : public ActionPrimitive<> {
  void operator ()() {
    // nothing
  }
};

REGISTER_PRIMITIVE(no_op);

class execute_meter
  : public ActionPrimitive<MeterArray &, const Data &, Field &> {
  void operator ()(MeterArray &meter_array, const Data &idx, Field &dst) {
    dst.set(meter_array.execute_meter(get_packet(), idx.get_uint()));
  }
};

REGISTER_PRIMITIVE(execute_meter);

class count : public ActionPrimitive<CounterArray &, const Data &> {
  void operator ()(CounterArray &counter_array, const Data &idx) {
    counter_array.get_counter(idx.get_uint()).increment_counter(get_packet());
  }
};

REGISTER_PRIMITIVE(count);

class register_read
  : public ActionPrimitive<Field &, const RegisterArray &, const Data &> {
  void operator ()(Field &dst, const RegisterArray &src, const Data &idx) {
    dst.set(src[idx.get_uint()]);
  }
};

REGISTER_PRIMITIVE(register_read);

class register_write
  : public ActionPrimitive<RegisterArray &, const Data &, const Data &> {
  void operator ()(RegisterArray &dst, const Data &idx, const Data &src) {
    dst[idx.get_uint()].set(src);
  }
};

REGISTER_PRIMITIVE(register_write);

// I cannot name this "truncate" and register it with the usual
// REGISTER_PRIMITIVE macro, because of a name conflict:
//
// In file included from /usr/include/boost/config/stdlib/libstdcpp3.hpp:77:0,
//   from /usr/include/boost/config.hpp:44,
//   from /usr/include/boost/cstdint.hpp:36,
//   from /usr/include/boost/multiprecision/number.hpp:9,
//   from /usr/include/boost/multiprecision/gmp.hpp:9,
//   from ../../src/bm_sim/include/bm_sim/bignum.h:25,
//   from ../../src/bm_sim/include/bm_sim/data.h:32,
//   from ../../src/bm_sim/include/bm_sim/fields.h:28,
//   from ../../src/bm_sim/include/bm_sim/phv.h:34,
//   from ../../src/bm_sim/include/bm_sim/actions.h:34,
//   from primitives.cpp:21:
//     /usr/include/unistd.h:993:12: note: declared here
//     extern int truncate (const char *__file, __off_t __length)
class truncate_ : public ActionPrimitive<const Data &> {
  void operator ()(const Data &truncated_length) {
    get_packet().truncate(truncated_length.get<size_t>());
  }
};

REGISTER_PRIMITIVE_W_NAME("truncate", truncate_);

// dummy function, which ensures that this unit is not discarded by the linker
// it is being called by the constructor of SimpleSwitch
// the previous alternative was to have all the primitives in a header file (the
// primitives could also be placed in simple_switch.cpp directly), but I need
// this dummy function if I want to keep the primitives in their own file
int import_primitives() {
  return 0;
}
