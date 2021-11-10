package Tests;

import Server.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Cont_Requests {
    public static void main(String[] args) throws Exception{
        Socket soc = new Socket("127.0.0.1", 5000);
        ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
        ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());

        for (int i = 0; i < 6; i++) {
            if(i == 3 || i == 5)out.writeObject(new Message<String>( "RequestVote", "comando" + i));
            else out.writeObject(new Message<String>( "AppendEntry", "comando" + i));
        }

        out.close();
        inp.close();
        soc.close();
    }
}
