package Server;

import java.io.Serializable;

public class Log implements Serializable{
    public int term;
    public String command;

    public Log(int term, String command) {
        this.term = term;
        this.command = command;
    }

    @Override
    public String toString(){
        return term + ";" + command;
    }
}
