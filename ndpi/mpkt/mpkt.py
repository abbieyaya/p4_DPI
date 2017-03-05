'''
Mazi Packet Module
'''

from socket import inet_ntoa
import dpkt

class Enum(object):
    def __getattr__(self, name):
        if name in self:
            return name
        raise AttributeError

''' Connection state '''
class _ConState(Enum):
    DISCONNECTED    = 0     # no connection or disconnected
    LISTEN          = 1     # waiting for connection
    SYN_SENT        = 2     # TCP handshake, after SYN
    SYN_RECEIVED    = 3     # TCP handshake, after SYN ACK
    ESTABLISHED     = 4     # connected, after handshake: ACK
    FIN_WAIT_1      = 5     # not used
    FIN_WAIT_2      = 6     # not used
    CLOSE_WAIT      = 7     # not used
    CLOSING         = 8     # not used
    LAST_ACK        = 9     # not used
    TIME_WAIT       = 10    # not used
    CLOSED          = 11    # not used

''' Application round state '''
class _APPRState(Enum):
    WAITTING    = 0     # Waiting for first pakcet
    TALK_A_C2S  = 1     # Talk A, client to server
    TALK_A_S2C  = 2     # Talk A, server to client
    TALK_B_S2C  = 3     # Talk B, server to client
    TALK_B_C2S  = 4     # Talk B, client to server
    TERMINAL    = 5     # End of application round

''' SSL state '''
class SSLState(Enum):
    DISCONNECTED    = 0
    CLIENT_HELLO    = 1
    SERVER_HELLO    = 2
    CLIENT_KEYEX    = 3
    CLIENT_FINISHED = 4
    SERVER_FINISHED = 5
    EXCHANGE_MESS   = 6
    CLOSED          = 7

''' Exception for packet format '''
class PacketError(Exception):
    def __init__(self, message):
        # Call the base class constructor with the parameters it needs
        super(PacketError, self).__init__(message)


''' 5-tuple, protocol is omissible (TCP) '''
class FiveTuple(object):
    def __init__(self, src, dst, sport, dport, proto = 'TCP'):
        self.src = src      # Source IP address
        self.dst = dst      # Destination IP address
        self.sport = sport  # Source TCP port
        self.dport = dport  # Destination TCP port
        self.proto = proto  # Protocol

    ''' Tell whether another 5-tuple is equal or not '''
    def equal(self, tar):
        if self.src != tar.src or self.sport != tar.sport: return False
        if self.dst != tar.dst or self.dport != tar.dport: return False
        if self.proto != tar.proto: return False
        return True

    ''' Tell whether the reversal of another 5-tuple is equal or not '''
    def equalRev(self, tar):
        return self.equal(tar.reversal())

    ''' Return reversal of the connection '''
    def reversal(self):
        return FiveTuple(self.dst, self.src, self.dport, self.sport, self.proto)

    ''' Return tuple string '''
    def toString(self):
        return str(self.src)+':'+str(self.sport)+' > '+str(self.dst)+':'+str(self.dport)


''' TCP Connection '''
class Connection(FiveTuple):
    def __init__(self, ts, src, dst, sport, dport, proto = 'TCP'):
        super(Connection, self).__init__(src, dst, sport, dport, proto)
        self.l5_proto = 'SSL' if dport == 443 else None     # SSL Judgement, TODO: Need a better solution
        self.time = {
            'starting':   ts                    # Starting timestamp of connection
        }
        self.count = {
            'packet':   0,                      # Packet count
            'control':  0,                      # Control packet count
            'data':     0,                      # Data packet count
            'byte':     0                       # Packet size count
        }
        self.state = {
            'TCP':  _ConState.DISCONNECTED,     # TCP connection state
            'APPR': _APPRState.WAITTING,        # Application round state
            'SSL':  SSLState.DISCONNECTED       # SSL state
        }

    @classmethod
    def from5tuple(cls, ts, tuples):
        return cls(ts, tuples.src, tuples.dst, tuples.sport, tuples.dport, tuples.proto)

    ''' Process next packet to see if there is any state change, then return it '''
    def next(self, packet):
        alters = {}
        if not self.belong(packet.get5tuple()): return None

        # Prepare states change
        alters['TCP']   = self._next_TCP(packet)
        alters['APPR']  = self._next_APPR(packet)
        if self.l5_proto == 'SSL':
            alters['SSL'] = self._next_SSL(packet)

        # Packet counting
        self.count['packet']    += 1
        self.count['control']   += 1 if packet.len == 0 else 0
        self.count['data']      += 1 if packet.len != 0 else 0
        self.count['byte']      += packet.len

        # Change states
        for key, val in alters.iteritems():
            if val: self.state[key] = val

        return alters

    ''' Process next packet, TCP handshake part '''
    def _next_TCP(self, packet):
        # State DISCONNECTED
        if self.state['TCP'] == _ConState.DISCONNECTED:
            if not self.equal(packet.get5tuple()): return None
            if not packet.isFlags('SYN'): return None
            return _ConState.SYN_SENT
        # State SYN-SENT
        elif self.state['TCP'] == _ConState.SYN_SENT:
            if not self.equalRev(packet.get5tuple()): return None
            if not packet.isFlags('SYN', 'ACK'): return None
            return _ConState.SYN_RECEIVED
        # State SYN-RECEIVED
        elif self.state['TCP'] == _ConState.SYN_RECEIVED:
            if not self.equal(packet.get5tuple()): return None
            if not packet.isFlags('ACK'): return None
            return _ConState.ESTABLISHED
        # Otherwise
        else:
            return None
        return None

    ''' Process next packet, application round part '''
    def _next_APPR(self, packet):
        # Only process established connections
        if self.state['TCP'] != _ConState.ESTABLISHED: return None
        if self.l5_proto == 'SSL' and self.state['SSL'] != SSLState.EXCHANGE_MESS: return None
        if packet.len <= 0: return None     # Skip control message

        # First packet
        if self.state['APPR'] == _APPRState.WAITTING:
            if self.equal(packet.get5tuple()): return _APPRState.TALK_A_C2S
            elif self.equalRev(packet.get5tuple()): return _APPRState.TALK_A_S2C
        # Talk A, client to server
        elif self.state['APPR'] == _APPRState.TALK_A_C2S:
            if self.equalRev(packet.get5tuple()): return _APPRState.TALK_B_S2C
        # Talk A, server to client
        elif self.state['APPR'] == _APPRState.TALK_A_S2C:
            if self.equal(packet.get5tuple()): return _APPRState.TALK_B_C2S
        # Talk B, server to client
        elif self.state['APPR'] == _APPRState.TALK_B_S2C:
            if self.equal(packet.get5tuple()): return _APPRState.TERMINAL
        # Talk B, client to server
        elif self.state['APPR'] == _APPRState.TALK_B_C2S:
            if self.equalRev(packet.get5tuple()): return _APPRState.TERMINAL

        return None

    ''' Process next pakcet, SSL part '''
    ''' Use 4-way handshake but not header bytes to check, becuase TCP is stream. '''
    def _next_SSL(self, packet):
        # Only process established connections
        if self.state['TCP'] != _ConState.ESTABLISHED: return None
        if packet.len <= 0: return None     # Skip control message

        # State Disconnected
        if self.state['SSL'] == SSLState.DISCONNECTED:
            if self.equalRev(packet.get5tuple()): return SSLState.SERVER_HELLO
        # State
        elif self.state['SSL'] == SSLState.SERVER_HELLO:
            if self.equal(packet.get5tuple()): return SSLState.CLIENT_KEYEX
        # State
        elif self.state['SSL'] == SSLState.CLIENT_KEYEX:
            if self.equalRev(packet.get5tuple()): return SSLState.SERVER_FINISHED
        # State
        elif self.state['SSL'] == SSLState.SERVER_FINISHED:
            return SSLState.EXCHANGE_MESS

        return None

    ''' Tell whether the 5-tuple is belong to this connection '''
    def belong(self, tar_tuple):
        this_tuple = self.get5tuple()
        return this_tuple.equal(tar_tuple) or this_tuple.equalRev(tar_tuple)

    ''' Downcast to connection '''
    def get5tuple(self):
        return FiveTuple(self.src, self.dst, self.sport, self.dport, self.proto)


''' TCP Packet '''
class Packet(FiveTuple):
    ''' mpkt expects a TCP packet buffer '''
    def __init__(self, buf):
        # Check this packet is TCP or not
        try:
            self.eth = dpkt.ethernet.Ethernet(buf)

            self.ip = self.eth.data
            if type(self.ip) != dpkt.ip.IP:
                raise PacketError("This is not a IP packet.")

            self.tcp = self.ip.data
            if type(self.tcp) != dpkt.tcp.TCP:
                raise PacketError("This is not a TCP packet.")
        except:
            raise PacketError("This is not a TCP packet.")

        # Member varible
        super(Packet, self).__init__(
            inet_ntoa(self.ip.src), inet_ntoa(self.ip.dst),
            self.tcp.sport, self.tcp.dport, 'TCP'
        )
        self.len = self.ip.len - self.ip.hl*4 - self.tcp.off*4
        #self.eth                           # dpkt ethernet object
        #self.ip                            # dpkt IP object
        #self.tcp                           # dpkt TCP object
        self.flags = {                      # TCP flags
            'FIN': bool(self.tcp.flags & dpkt.tcp.TH_FIN),
            'SYN': bool(self.tcp.flags & dpkt.tcp.TH_SYN),
            'RST': bool(self.tcp.flags & dpkt.tcp.TH_RST),
            'PSH': bool(self.tcp.flags & dpkt.tcp.TH_PUSH),
            'ACK': bool(self.tcp.flags & dpkt.tcp.TH_ACK),
            'URG': bool(self.tcp.flags & dpkt.tcp.TH_URG),
            'ECE': bool(self.tcp.flags & dpkt.tcp.TH_ECE),
            'CWR': bool(self.tcp.flags & dpkt.tcp.TH_CWR)
        }

    ''' Downcast to connection '''
    def get5tuple(self):
        return FiveTuple(self.src, self.dst, self.sport, self.dport, self.proto)

    ''' Check the packet flags '''
    def isFlags(self, *args):
        for flag, val in self.flags.iteritems():
            if flag in args and not val:
                return False
            elif flag not in args and val:
                return False
        return True
