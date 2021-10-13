package Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Thread_Connect_Receiver extends Thread{
    public Socket soc;
    public PriorityBlockingQueue<Message> main_queue;
    public AtomicBoolean alive;

    public Thread_Connect_Receiver(Socket soc, PriorityBlockingQueue<Message> queue){
        this.soc = soc;
        this.main_queue = queue;
        this.alive = new AtomicBoolean(false);
    }

    @Override
    public void run(){
        try{
            this.alive.set(true);
            ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
            while(this.alive.get()){
                main_queue.add((Message)inp.readObject());
            }
            inp.close();
            soc.close();
        }
        catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }
}
