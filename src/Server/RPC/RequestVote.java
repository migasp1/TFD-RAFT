package Server.RPC;

import java.io.Serializable;

public class RequestVote implements Serializable {
    public int term, candidateId, lastLogIndex, lastLogTerm;

    public RequestVote(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }
}
