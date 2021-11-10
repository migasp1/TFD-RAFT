package Server.RPC;

import java.io.Serializable;

public class RequestVoteReply implements Serializable {
    public int term;
    public boolean voteGranted;

    public RequestVoteReply(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }
}
