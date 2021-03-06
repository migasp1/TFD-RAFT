package Server.Threads;

import Server.Message;
import Server.RAFTMessagePriorityBlockingQueue;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Thread_Connect_Receiver extends Thread{
    public int me, he, port;
    public RAFTMessagePriorityBlockingQueue<Message> main_queue;
    public AtomicBoolean alive;

    public Thread_Connect_Receiver(int port, RAFTMessagePriorityBlockingQueue<Message> queue,  int me, int he, AtomicBoolean alive){
        this.port = port;
        this.main_queue = queue;
        this.me = me;
        this.he = he;
        this.alive = alive;
    }

    @Override
    public void run(){
        ServerSocket serv = null;
        Socket soc = null;
        ObjectInputStream inp = null;
        while(true) {
            try {
                this.alive.set(true);
                serv = new ServerSocket(Integer.parseInt(30 + "" + me + "" + he));
                soc = serv.accept();
                inp = new ObjectInputStream(soc.getInputStream());
                while (this.alive.get()) {
                    main_queue.add((Message)inp.readObject());
                }
                inp.close();
                soc.close();
                serv.close();
            } catch (Exception ex) {
                System.err.println(ex.getMessage() + " receiver replica " + he);
                this.alive.set(false);
            }
            try {
                if(inp  != null)inp.close();
                if(soc != null)soc.close();
                if(serv != null)serv.close();
            }catch (Exception ex){
                System.err.println(ex.getMessage());
                this.alive.set(false);
            }
        }
    }
}
