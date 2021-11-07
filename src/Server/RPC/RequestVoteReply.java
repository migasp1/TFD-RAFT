package Server.RPC;

public class RequestVoteReply {
    public int term;
    public boolean voteGranted;

    public RequestVoteReply(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }
}
