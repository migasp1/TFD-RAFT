package Tests;

import Server.Message;
import Server.RAFTMessagePriorityBlockingQueue;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Test_Message_Queue {
    public static void main(String[] args) throws Exception{

        RAFTMessagePriorityBlockingQueue<Message> queue = new RAFTMessagePriorityBlockingQueue();

        ServerSocket serv = new ServerSocket(5000);
        Socket soc = serv.accept();
        ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());

        for (int i = 0; i < 6; i++) {
            queue.add((Message)inp.readObject());
            System.out.println(queue);
        }

        out.close();
        inp.close();
        soc.close();

    }
}
