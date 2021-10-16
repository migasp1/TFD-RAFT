package Server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Thread_Majority_Invoke extends Thread{

    public PriorityBlockingQueue main_queue;
    public PriorityBlockingQueue<Message>[] thread_queue;
    public BlockingQueue<Message> mi_queue;
    public Message m;
    public AtomicBoolean alive;

    public Thread_Majority_Invoke(PriorityBlockingQueue main_queue, PriorityBlockingQueue<Message>[] thread_queue, BlockingQueue<Message> mi_queue, Message m){
        this.main_queue = main_queue;
        this.thread_queue = thread_queue;
        this.mi_queue = mi_queue;
        this.m = m;
        this.m.label = "RequestVote";
        this.alive = new AtomicBoolean(false);
    }


    public boolean isProcessing(){return alive.get();}

    @Override
    public void run(){
        for (int i = 0; i < thread_queue.length; i++) {
            thread_queue[i].add(m);
        }
        double min = thread_queue.length/2.0;
        double i = 0;
        Message [] res = new Message[thread_queue.length];
        alive.set(true);
        while(Double.compare(i, min) <= 0){
            while(mi_queue.size() != 0) {
                Message m = mi_queue.remove();
                res[m.senderID] = m;
                i++;
            }
        }
        alive.set(false);
        mi_queue.clear();
        Message<Message []>  m = new Message<Message[]>("LeaderElection", res);
        main_queue.add(m);
    }
}
