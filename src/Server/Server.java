package Server;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class Server {

    public List<String> servers;

    public String me;
    public int myReplicaID;

    public Map<String, ProcessRequest> requestHandlers;

    public Thread_Connect_Sender[] senders;
    public Thread_Connect_Receiver[] receivers;
    public Thread_Majority_Invoke mj;

    public PriorityBlockingQueue<Message> main_queue;
    public PriorityBlockingQueue<Message>[] thread_queue;
    public BlockingQueue<Message> mi_queue;


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
        this.mi_queue = new LinkedBlockingQueue<Message>();
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
            System.out.println("All connections established!");
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void invoke(int replicaId, Message m) {
        thread_queue[replicaId].add(m);
    }

    public void registerHandler(String requestLabel, ProcessRequest processRequest){
        requestHandlers.put(requestLabel, processRequest);
    }

    public void majorityInvoke(Message m){
        if(mj == null || !mj.isAlive()) {
            this.mj = new Thread_Majority_Invoke(this.main_queue, this.thread_queue, this.mi_queue, m);
            this.mj.start();
        }
    }

    //aqui vai ser onde vamos defenir o algoritmo
    public void execute() {
        /*invoke*/
         if (myReplicaID == 0) {
             while (true) {
                 while (main_queue.size() != 0) {
                     Message m = main_queue.remove();
                     if (m.label.equals("ClientRequest")) {
                         m.label = "AppendEntries";
                         m.senderID = myReplicaID;
                         invoke(1, m);
                     }
                     else if(m.label.equals("AppendEntriesReply")){
                         System.out.println((String)m.data);
                     }
                 }
             }
         }
         else{
             while (true) {
                 while (main_queue.size() != 0) {
                     Message m = main_queue.remove();
                     if (m.label.equals("AppendEntries")) {
                         invoke(m.senderID, new Message("AppendEntriesReply", "adeus", myReplicaID));
                     }
                 }
             }
         }



        /* majorityInvoke
        if (myReplicaID == 0) {
            while (true) {
                while (main_queue.size() != 0) {
                    Message m = main_queue.remove();
                    if(m.label.equals("RequestVote"))invoke(m.senderID, new Message("RequestVoteReply", "Positivo"));
                    else if(m.label.equals("RequestVoteReply") && mj.isProcessing())mi_queue.add(m);
                    else if(m.label.equals("LiderElection")){
                        String [] res = (String [])m.data;
                        System.out.println("");
                        for (int i = 0; i < res.length; i++) {
                            System.out.println(i + " " +res[i]);
                        }
                    }
                    else if(m.label.equals("ClientRequest")){
                        majorityInvoke(m);
                    }
                }
            }
        } else if (myReplicaID >= 1) {
            while (true) {
                while (main_queue.size() != 0) {
                    Message m = main_queue.remove();
                    invoke(m.senderID, new Message("RequestVoteReply", "Positivo", myReplicaID));
                }
            }
        }
        */
    }
}