package Server;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class Server {

    public List<String> servers;

    public String me;
    public int myReplicaID;

    public Map<String, ProcessRequest> requestHandlers;

    public Thread_Connect_Sender[] senders;
    public Thread_Connect_Receiver[] receivers;

    public PriorityBlockingQueue<Message> main_queue;
    public PriorityBlockingQueue<Message> [] thread_queue;

    public Thread_Client_Receiver client_receiver;

    public Server(String file, int replicaID){
        this.myReplicaID = replicaID;
        this.servers = new ArrayList<>();
        this.requestHandlers = new HashMap<>();
        this.main_queue = new PriorityBlockingQueue<>();
        try {
            Scanner reader = new Scanner(new File(file));
            int  i = 0;
            while (reader.hasNextLine()) {
                if(i != myReplicaID)this.servers.add(reader.nextLine());
                else this.me = reader.nextLine();
                i++;
            }
            reader.close();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        this.senders = new Thread_Connect_Sender[servers.size()];
        this.receivers = new Thread_Connect_Receiver[servers.size()];
        this.thread_queue = new PriorityBlockingQueue[servers.size()];
        create_connection();
    }

    public void create_connection(){
        try{
            for (int i = 0; i < senders.length; i++) {
                thread_queue[i] = new PriorityBlockingQueue<Message>();
                senders[i] = new Thread_Connect_Sender(servers.get(i), thread_queue[i]);
                senders[i].setDaemon(true);
                senders[i].start();
            }
            ServerSocket serv = new ServerSocket(Integer.parseInt(me.split(":")[1]));
            for (int i = 0; i < receivers.length; i++) {
                Socket soc = serv.accept();
                receivers[i] = new Thread_Connect_Receiver(soc, main_queue);
                receivers[i].setDaemon(true);
                receivers[i].start();
            }
            serv.close();
            this.client_receiver = new Thread_Client_Receiver(Integer.parseInt(me.split(":")[1]), this.main_queue);
            this.client_receiver.setDaemon(true);
            this.client_receiver.start();
        }
        catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }

    //aqui vai ser onde vamos defenir o algoritmo
    public void execute(){
        while(true){
            while(main_queue.size() != 0){
                Message m  = main_queue.remove();
                for (int i = 0; i < thread_queue.length; i++) {
                    thread_queue[i].add(m);
                }
            }
        }
    }

}