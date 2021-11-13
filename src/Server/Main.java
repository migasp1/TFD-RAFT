package Server;

public class Main {

    public static void main(String[] args) {
        Server ser;
        if(args.length > 2)ser = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        else ser = new Server(args[0], Integer.parseInt(args[1]));
        ser.execute();
    }
}
