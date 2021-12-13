package Server.Threads;

import Server.Message;
import Server.RAFTMessagePriorityBlockingQueue;
import Server.RPC.ClientRequestReply;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Thread_Client_Sender extends Thread{

    public RAFTMessagePriorityBlockingQueue<Message> thread_queue;

    public Thread_Client_Sender(RAFTMessagePriorityBlockingQueue<Message> queue){
        this.thread_queue = queue;
    }

    @Override
    public void run(){
            while (true) {
                if (thread_queue.peek() != null) {
                    try {
                        Message m = thread_queue.remove();;
                        ClientRequestReply rep = (ClientRequestReply) m.data;
                        Socket soc = new Socket(rep.me_ip, rep.me_port);
                        ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
                        ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
                        out.writeObject(m);
                        out.close();
                        inp.close();
                        soc.close();
                    }
                    catch (IOException e) {
                        System.out.println("erro");
                        //e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
    }
}
