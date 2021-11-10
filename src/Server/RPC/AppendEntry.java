package Server.RPC;

import Server.Log;

import java.io.Serializable;

public class AppendEntry implements Serializable {
    public int term, leaderId, prevLogIndex, prevLogTerm, leaderCommit;
    public Log [] entries;

    public AppendEntry(int term, int leaderId, int prevLogIndex, int prevLogTerm, int leaderCommit, Log[] entries) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.leaderCommit = leaderCommit;
        this.entries = entries;
    }
}
