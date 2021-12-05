package Server;

import java.io.Serializable;

public class Message<T> implements Serializable, Comparable<Message>{
    public String label;
    public T data;
    public int senderID, seqNumber;

    public Message(String label, T data, int senderID, int seqNumber){
        this.label = label;
        this.data = data;
        this.senderID = senderID;
        this.seqNumber = seqNumber;
    }
    public Message(String label, T data){
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
        String [] order = new String[]{"RequestVote", "RequestVoteReply", "ClientRequest", "AppendEntry", "AppendEntryReply"};
        int a = 0;
        int b = 0;
        for (int i = 0; i < order.length; i++) {
            if(this.label.equals(order[i]))a = i;
            if(message.label.equals(order[i]))b = i;
        }
        return a == b ? 0 : a < b  ? -1 : 1;
    }

    @Override
    public String toString(){
        return label + " : " + data.toString();
    }
}
