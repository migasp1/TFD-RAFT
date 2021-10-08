package Client;

import Server.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws Exception{
        Socket soc = new Socket("127.0.0.1", 5000);
        ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
        ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());

        out.writeObject(new Message("ola", "amigo"));

        out.close();
        inp.close();
        soc.close();
    }
}
