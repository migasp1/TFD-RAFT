package Server;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable, Comparable<Message>{
    public String label;
    public String data;

    public Message(String label, String data){
        this.label = label;
        this.data = data;
    }

    /*
    Possiveis Cabecalhos por ordem:
    RequestVote
    ClientRequest
    AppendEntries
    */
    @Override
    public int compareTo(Message message) {
        String [] order = new String[]{"RequestVote", "ClientRequest", "AppendEntries"};
        int a = 0;
        int b = 0;
        for (int i = 0; i < order.length; i++) {
            if(this.label.equals(order[i]))a = i;
            if(message.label.equals(order[i]))b = i;
        }
        return a <= b ? a == b ? 0 : 1 : -1;//pode ser necessário trocar o -1  e o 1
    }
}
