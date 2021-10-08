package Server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Thread_Connect_Sender extends Thread{

    public String ips;
    public PriorityBlockingQueue<Message> thread_queue;
    public AtomicBoolean alive;

    public Thread_Connect_Sender(String m, PriorityBlockingQueue<Message> queue){
        this.ips = m;
        this.thread_queue = queue;
        this.alive = new AtomicBoolean(false);
    }

    @Override
    public void run(){
        this.alive.set(true);
        String [] ip = ips.split(":");
        try{
            Socket soc = null;
            while(true) {
                try {
                    soc = new Socket(ip[0], Integer.parseInt(ip[1]));
                    break;
                } catch (IOException ex) {
                    if(!alive.get())break;
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    throw new Exception(ex.getMessage());
                }
            }
            ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
            while(alive.get()){
                if(thread_queue.size() != 0){
                    Message m = thread_queue.remove();
                    out.writeObject(m);
                }
            }
            out.close();
            soc.close();
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}
