import socket
import sys
import time
import pickle
import csv

fo = None
fo_writer = None

def dict2csv(src, dst, protocol, label):
    global fo_writer, fo
    result = []
    result.append(src)
    result.append(dst)
    result.append(protocol)
    print label
    print label.find('.')
    if label.find('.') > 0 :
        temp = label.split('.');
        result.append(temp[0])
        result.append(temp[1])
    else :
        result.append(label)

    print result
    fo_writer.writerow(result)
    fo.flush()

def main():
    global fo_writer, fo
    print len(sys.argv)
    if len(sys.argv) == 1:
        file_out = 'essence.csv'
    elif len(sys.argv) == 2 :
        file_out = sys.argv[1]
    else :
        print "Usage sudo ./server.py [output.csv]"

    try :
        fo = open(file_out, 'w')
        fo_writer = csv.writer(fo)
    except IOError as (errno, strerror):
        print "I/O error({0}): {1}".format(errno, strerror)
        return

    # Create a TCP/IP socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # Bind the socket to the port
    server_address = ("192.168.164.131", 10000)
    print >>sys.stderr, 'starting up on %s port %s' % server_address
    sock.bind(server_address)

    # Listen for incoming connections
    sock.listen(1)

    while True:
        # Wait for a connection
        print >>sys.stderr, 'waiting for a connection'
        connection, client_address = sock.accept()
        try:
            print >>sys.stderr, 'connection from', client_address

            # Receive the data in small chunks and retransmit it
            while True:
                #data = connection.recv(1024)
                data = pickle.loads(connection.recv(1024))
                if data :
                    dict2csv(data[0],data[1],data[2],data[3])

                else:
                    print >>sys.stderr, 'no more data from', client_address
                    time.sleep(10)
                    #break

        finally:
            # Clean up the connection
            connection.close()

if __name__ == '__main__':
    main()
