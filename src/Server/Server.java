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
    public PriorityBlockingQueue<Message>[] thread_queue;

    public Thread_Client_Receiver client_receiver;

    public Server(String file, int replicaID) {
        this.myReplicaID = replicaID;
        this.servers = new ArrayList<>();
        this.requestHandlers = new HashMap<>();
        this.main_queue = new PriorityBlockingQueue<>();
        try {
            Scanner reader = new Scanner(new File(file));
            int i = 0;
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (i == replicaID)this.me = (line);
                this.servers.add(line);
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

    public void create_connection() {
        try {
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
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void invoke(int replicaId, Message m) {
        thread_queue[replicaId].add(m);
    }

    public void registerHandler(String requestLabel, ProcessRequest processRequest){
        this.requestHandlers.put(requestLabel, processRequest);
    }

    public void majorityInvoke(Message m){
        for (int i = 0; i < servers.size(); i++) {
            if(i != myReplicaID)invoke(i, m);
        }
    }

    //aqui vai ser onde vamos defenir o algoritmo
    public void execute() {
        if (myReplicaID == 0) {
            while (true) {
                while (main_queue.size() != 0) {
                    Message m = main_queue.remove();
                    invoke(1, m);
                }
            }
        } else if (myReplicaID == 1) {
            while (true) {
                while (main_queue.size() != 0) {
                    Message m = main_queue.remove();
                    System.out.println(m.data);
                }
            }
        }
        Message m = new Message("a","a");
        ProcessRequest proc = this.requestHandlers.get(m.label);
        proc.processRequest(1, m);
    }
}