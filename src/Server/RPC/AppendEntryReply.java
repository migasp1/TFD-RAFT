package Server.RPC;

public class AppendEntryReply {
    public int term;
    public boolean success;

    public AppendEntryReply(int term, boolean success) {
        this.term = term;
        this.success = success;
    }
}
