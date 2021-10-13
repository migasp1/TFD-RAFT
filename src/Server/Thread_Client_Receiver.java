package Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;

public class Thread_Client_Receiver extends Thread{

    public int port;
    public PriorityBlockingQueue<Message> main_queue;

    public Thread_Client_Receiver(int port, PriorityBlockingQueue<Message> queue){
        this.port = port;
        this.main_queue = queue;
    }

    @Override
    public void run(){
        try{
            ServerSocket serv = new ServerSocket(this.port);
            for (int i = 0; i < 10000; i++) {//-------------------------------------------------trocar por while infinito com condição de saida
                Socket soc = serv.accept();
                ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());

                main_queue.add((Message)inp.readObject());

                inp.close();
                out.close();
                soc.close();
            }
            serv.close();
        }
        catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }
}
