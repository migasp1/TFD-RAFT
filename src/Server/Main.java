package Server;

public class Main {

    public static void main(String[] args) {
        Server ser = new Server(args[0], Integer.parseInt(args[1]));

        ser.execute();
    }
}
