package Server.RPC;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class ClientRequest implements Serializable {

    public String me_ip;
    public int me_port;
    public int seq;
    public byte [] data;

    public ClientRequest(String me_ip, int me_port, int seq, byte[] data) {
        this.me_ip = me_ip;
        this.me_port = me_port;
        this.seq = seq;
        this.data = data;
    }

    public String getData(){
        return new String(data, StandardCharsets.UTF_8) ;
    }

    @Override
    public String toString(){
        return (new String(data, StandardCharsets.UTF_8));
    }
}
