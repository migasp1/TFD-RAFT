package Server.Threads;

import Server.Message;
import Server.RAFTMessagePriorityBlockingQueue;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Thread_Client_Receiver extends Thread{

    public int port;
    public RAFTMessagePriorityBlockingQueue<Message> main_queue;

    public Thread_Client_Receiver(int port, RAFTMessagePriorityBlockingQueue<Message> queue){
        this.port = port;
        this.main_queue = queue;
    }

    @Override
    public void run(){
        try{
            ServerSocket serv = new ServerSocket(this.port);
            while(true){
                Socket soc = serv.accept();

                ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());

                main_queue.add((Message)inp.readObject());

                inp.close();
                out.close();
                soc.close();
            }
//          serv.close();
        }
        catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }
}
