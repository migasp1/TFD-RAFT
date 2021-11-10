package Server.Threads;

import Server.Message;
import Server.RAFTMessagePriorityBlockingQueue;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Thread_Connect_Sender extends Thread{

    public String ip;
    public RAFTMessagePriorityBlockingQueue<Message> thread_queue;
    public AtomicBoolean alive;
    int me, he, port;

    public Thread_Connect_Sender(String ip, int port, RAFTMessagePriorityBlockingQueue<Message> queue, int me, int he, AtomicBoolean alive){
        this.ip = ip;
        this.port = port;
        this.thread_queue = queue;
        this.me = me;
        this.he = he;
        this.alive = alive;
    }

    @Override
    public void run(){
        while(true) {
            Socket soc = null;
            ObjectOutputStream out = null;
            this.alive.set(true);
            try {
                soc = null;
                //System.out.println("attack: " + 30 + "" + he + "" + me);
                while (soc == null) {
                    try {
                        soc = new Socket(ip, Integer.parseInt(30 + "" + he + "" + me));
                    } catch (IOException ex) {
                        Thread.sleep(1000);
                    }
                }
                out = new ObjectOutputStream(soc.getOutputStream());
                //System.out.println("ok1");
                while (alive.get()) {
                    if (thread_queue.size() != 0) {
                        Message m = thread_queue.peek();
                        out.writeObject(m);
                        thread_queue.remove();
                    }
                }
                out.close();
                soc.close();
            } catch (Exception ex) {
                System.err.println(ex.getMessage() + " sender replica " + he);
                this.alive.set(false);
            }
            try {
                if(out  != null)out.close();
                if(soc != null)soc.close();
            }catch (Exception ex){
                System.err.println(ex.getMessage());
                this.alive.set(false);
            }
            //System.out.println("fim1");
        }
    }
}
