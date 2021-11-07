package Server;

public class Log {
    public int term, index;
    public String command;

    public Log(int term, String command) {
        this.term = term;
        this.command = command;
    }
}
