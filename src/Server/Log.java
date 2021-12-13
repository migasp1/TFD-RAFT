package Server;

import java.io.Serializable;

public class Log implements Serializable{
    public int term, port, seq;
    public String command, ip;

    public Log(int term, String command, String ip, int port, int seq) {
        this.term = term;
        this.command = command;
        this.ip = ip;
        this.port = port;
        this.seq = seq;
    }

    @Override
    public String toString(){
        return term + ";" + command + ";" + ip + ";" + port + ";" + seq;
    }
}
