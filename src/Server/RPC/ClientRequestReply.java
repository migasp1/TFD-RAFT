package Server.RPC;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class ClientRequestReply implements Serializable {

    public String me_ip;
    public int me_port;
    public int seq;
    public byte [] data;

    public ClientRequestReply(String me_ip, int me_port, int seq, byte [] data) {
        this.me_ip = me_ip;
        this.me_port = me_port;
        this.seq = seq;
        this.data = data;
    }

    @Override
    public String toString() {
        return (new String(data, StandardCharsets.UTF_8));
    }

}
