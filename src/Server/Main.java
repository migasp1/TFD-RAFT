package Server;

import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        ServerLibrary ser;
        if(args.length > 2)ser = new ServerLibrary(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        else ser = new ServerLibrary(args[0], Integer.parseInt(args[1]));
        ser.registerHandler(new ProcessRequest() {
            @Override
            public byte [] exe(Log req) {
                int c = Integer.parseInt(req.command);
                c++;
                return (c + "").getBytes(StandardCharsets.UTF_8);
            }
        });
        ser.execute();
    }
}
