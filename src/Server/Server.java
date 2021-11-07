package Server;

import Server.RPC.AppendEntry;
import Server.RPC.AppendEntryReply;
import Server.RPC.RequestVote;
import Server.RPC.RequestVoteReply;
import Server.Threads.Thread_Client_Receiver;
import Server.Threads.Thread_Connect_Receiver;
import Server.Threads.Thread_Connect_Sender;
import Server.Threads.Thread_Majority_Invoke;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {


    public int currentTerm, votedFor, commitIndex, lastApplied;//, lastLogIndex;
    public ArrayList<Log> log;

    public int[] nextLogEntry, matchIndex;


    long max_time;


    public List<String> servers;

    public String me;
    public int myReplicaID, state;

    public Map<String, ProcessRequest> requestHandlers;

    public Thread_Connect_Sender[] senders;
    public Thread_Connect_Receiver[] receivers;
    public Thread_Majority_Invoke mj;

    public PriorityBlockingQueue<Message> main_queue;
    public PriorityBlockingQueue<Message>[] thread_queue;
    public BlockingQueue<Message> mi_queue;

    public Thread_Client_Receiver client_receiver;

    public AtomicBoolean[] alive;

    public Server(String file, int replicaID) {
        this.myReplicaID = replicaID;
        this.state = 2;
        this.servers = new ArrayList<>();
        this.requestHandlers = new HashMap<>();
        this.main_queue = new PriorityBlockingQueue<>();
        try {
            Scanner reader = new Scanner(new File(file));
            int i = 0;
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (i == replicaID) this.me = (line);
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
        this.alive = new AtomicBoolean[servers.size()];

        this.currentTerm = 0;
        this.votedFor = -1;
        this.commitIndex = 0;
        this.lastApplied = 0;
        //this.lastLogIndex = 0;
        this.log = new ArrayList();
        this.nextLogEntry = new int[servers.size()];
        this.matchIndex = new int[servers.size()];

        Random rand = new Random();
        this.max_time = rand.nextInt(11) + 5;

        create_connection();
    }

    public void create_connection() {
        try {
            for (int i = 0; i < senders.length; i++) {
                String[] ips = servers.get(i).split(":");
                thread_queue[i] = new PriorityBlockingQueue<Message>();
                this.alive[i] = new AtomicBoolean(true);
                senders[i] = new Thread_Connect_Sender(ips[0], Integer.parseInt(ips[1]), thread_queue[i], myReplicaID, i, alive[i]);
                senders[i].setDaemon(true);
                senders[i].start();
            }
            int myPort = Integer.parseInt(servers.get(myReplicaID).split(":")[1]);
            for (int i = 0; i < receivers.length; i++) {
                receivers[i] = new Thread_Connect_Receiver(myPort, main_queue, myReplicaID, i, alive[i]);
                receivers[i].setDaemon(true);
                receivers[i].start();
            }
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

    public void registerHandler(String requestLabel, ProcessRequest processRequest) {
        requestHandlers.put(requestLabel, processRequest);
    }

    public void majorityInvoke(Message m) {
        /*if(mj == null || !mj.isAlive()) {
            this.mj = new Thread_Majority_Invoke(this.main_queue, this.thread_queue, this.mi_queue, m);
            this.mj.start();
        }*/

        for (int i = 0; i < thread_queue.length; i++) {
            if (i != myReplicaID) thread_queue[i].add(m);
        }
    }

    //aqui vai ser onde vamos defenir o algoritmo
    public void execute() {
        while (true) {
            if (state == 0) {//leader

            } else if (state == 1) {//candidato
                currentTerm++;
                Boolean[] votos = new Boolean[servers.size()];
                votos[myReplicaID] = true;
                RequestVote rv = new RequestVote(currentTerm, myReplicaID, lastApplied, log.get(log.size() - 1).term);
                long initial_time = System.currentTimeMillis();
                majorityInvoke(new Message("RequestVote", rv, myReplicaID));
                while (System.currentTimeMillis() <= initial_time + max_time * 1000) {
                    while (main_queue.size() != 0 && System.currentTimeMillis() <= initial_time + max_time * 1000) {
                        Message m = main_queue.peek();
                        if (System.currentTimeMillis() >= initial_time + max_time * 1000) break;
                        if (m.label.equals("RequestVoteReply")) {
                            RequestVoteReply pac = (RequestVoteReply) m.data;
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                state = 2;
                                initial_time = 0;
                            } else if (pac.term == currentTerm) {
                                votos[m.senderID] = pac.voteGranted;
                                int pos = 0, neg = 0;
                                for (int i = 0; i < servers.size(); i++) {
                                    if (votos[i] != null) {
                                        if (votos[i]) pos++;
                                        else neg++;
                                    }
                                }
                                if (pos > ((double) servers.size() / 2)) {
                                    state = 0;
                                    initial_time = 0;
                                } else if (neg > ((double) servers.size() / 2)) {
                                    state = 2;
                                    initial_time = 0;
                                }
                            }

                        } else if (m.label.equals("AppendEntry")) {
                            AppendEntry pac = (AppendEntry) m.data;
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                state = 2;
                                initial_time = 0;
                            }
                        }
                        main_queue.remove();
                    }
                }
            } else if (state == 2) {//seguidor
                long initial_time = System.currentTimeMillis();
                while (System.currentTimeMillis() <= initial_time + max_time * 1000) {
                    while (main_queue.size() != 0 && System.currentTimeMillis() <= initial_time + max_time * 1000) {
                        Message m = main_queue.peek();
                        if (m.label.equals("AppendEntry")) {
                            initial_time = System.currentTimeMillis();
                            AppendEntry pac = (AppendEntry) m.data;
                            AppendEntryReply pacReply = new AppendEntryReply(currentTerm, false);
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                state = 2;
                                initial_time = 0;
                            }
                            if (pac.term < currentTerm) pacReply.success = false;
                            else if (lastApplied < pac.prevLogIndex && currentTerm == pac.prevLogTerm)
                                pacReply.success = false;
                            else if (lastApplied == pac.prevLogIndex && currentTerm == pac.prevLogTerm) {//duvida
                                if (pac.leaderCommit > commitIndex)
                                    commitIndex = commitIndex < lastApplied ? lastApplied : commitIndex;
                                for (int i = 0; i < pac.entries.length; i++) {
                                    log.add(pac.entries[i]);
                                    lastApplied++;
                                }
                                pacReply.success = true;
                            } else if (lastApplied > pac.prevLogIndex && currentTerm == pac.prevLogTerm) {
                                for (int i = lastApplied; i > pac.prevLogIndex; i--) {
                                    this.log.remove(this.log.size() - 1);
                                }
                                //lastApplied = pac.prevLogIndex + 1;
                                for (int i = 0; i < pac.entries.length; i++) {
                                    log.add(pac.entries[i]);
                                    lastApplied++;
                                }
                                pacReply.success = true;
                            }
                            invoke(m.senderID, new Message<AppendEntryReply>("AppendEntryReply", pacReply, myReplicaID));
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm, false);
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                votedFor = -1;
                                state = 2;
                                initial_time = 0;//
                            }
                            if (pac.term < currentTerm) pacReply.voteGranted = false;
                            else {
                                if (votedFor == -1 && pac.term == currentTerm) { //&& lastApplied <= pac.lastLogIndex) {
                                    votedFor = pac.candidateId;
                                    pacReply.voteGranted = true;
                                } else pacReply.voteGranted = false;
                            }
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID));
                        }
                        main_queue.remove();
                    }
                }
            }
        }
    }
}



        /*invoke
         if (myReplicaID == 0) {
             while (true) {
                 while (main_queue.size() != 0) {
                     Message m = main_queue.remove();
                     if (m.label.equals("ClientRequest")) {
                         m.label = "AppendEntry";
                         m.senderID = myReplicaID;
                         invoke(1, m);
                     }
                     else if(m.label.equals("AppendEntryReply")){
                         System.out.println((String)m.data);
                     }
                 }
             }
         }
         else{
             while (true) {
                 while (main_queue.size() != 0) {
                     Message m = main_queue.remove();
                     if (m.label.equals("AppendEntry")) {
                         invoke(m.senderID, new Message("AppendEntryReply", "adeus", myReplicaID));
                     }
                 }
             }
         }
        */

        /*registerHandler_Invoke
        while(true){
            while(main_queue.size() != 0){
                Message m = main_queue.remove();
                ProcessRequest proc = requestHandlers.get(m.label);
                proc.exe(m);
            }
        }
        */

        /* majorityInvoke
        if (myReplicaID == 0) {
            while (true) {
                while (main_queue.size() != 0) {
                    Message m = main_queue.remove();
                    if(m.label.equals("RequestVote"))invoke(m.senderID, new Message("RequestVoteReply", "Positivo", myReplicaID));
                    else if(m.label.equals("RequestVoteReply") && mj.isProcessing())mi_queue.add(m);
                    else if(m.label.equals("LeaderElection")){
                        Message [] res = (Message [])m.data;
                        System.out.println("");
                        for (int i = 0; i < res.length; i++) {
                            System.out.println(i + " " + (res[i] == null ? "null" : res[i].data));
                        }
                    }
                    else if(m.label.equals("ClientRequest"))majorityInvoke(m);
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