package Server;

import Server.RPC.AppendEntry;
import Server.RPC.AppendEntryReply;
import Server.RPC.RequestVote;
import Server.RPC.RequestVoteReply;
import Server.Threads.Thread_Client_Receiver;
import Server.Threads.Thread_Connect_Receiver;
import Server.Threads.Thread_Connect_Sender;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {


    public int currentTerm, votedFor, commitIndex, lastApplied;
    public ArrayList<Log> log;

    public int[] nextLogEntry, matchIndex;

    int leader;
    long max_time;


    public List<String> servers;

    public String me;
    public int myReplicaID, state;

    //public Map<String, ProcessRequest> requestHandlers;

    public Thread_Connect_Sender[] senders;
    public Thread_Connect_Receiver[] receivers;

    public RAFTMessagePriorityBlockingQueue main_queue;
    public RAFTMessagePriorityBlockingQueue[] thread_queue;

    public Thread_Client_Receiver client_receiver;

    public AtomicBoolean[] alive;

    public Server(String file, int myReplicaID){
        this(file, myReplicaID, 0, 0);
    }

    public Server(String file, int replicaID, int currentTerm, int lastLogIndex) {
        this.myReplicaID = replicaID;
        this.state = 2;
        this.servers = new ArrayList<>();
        //this.requestHandlers = new HashMap<>();
        this.main_queue = new RAFTMessagePriorityBlockingQueue();
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
        this.thread_queue = new RAFTMessagePriorityBlockingQueue[servers.size()];
        this.alive = new AtomicBoolean[servers.size()];
        this.leader = 0;
        this.currentTerm = currentTerm;
        this.votedFor = -1;
        this.commitIndex = 0;
        this.lastApplied = lastLogIndex;
        this.log = new ArrayList();
        for (int i = 0; i < lastLogIndex + 1; i++) {
            log.add(new Log(currentTerm, "initial_command"));
        }

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
                thread_queue[i] = new RAFTMessagePriorityBlockingQueue<Message>();
                alive[i] = new AtomicBoolean(true);
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

    /*public void registerHandler(String requestLabel, ProcessRequest processRequest) {
        requestHandlers.put(requestLabel, processRequest);
    }*/

    public void majorityInvoke(Message m) {
        for (int i = 0; i < thread_queue.length; i++) {
            if (i != myReplicaID) thread_queue[i].add(m);
        }
    }

    //aqui vai ser onde vamos defenir o algoritmo
    public void execute() {
        while (true) {
            if (state == 0) {//leader
                long time = System.currentTimeMillis();
                AppendEntry ap = new AppendEntry(currentTerm, myReplicaID, lastApplied, log.get(log.size() - 1).term, commitIndex, new Log[0]);
                majorityInvoke(new Message("AppendEntry", ap, myReplicaID));
                for (int i = 0; i < nextLogEntry.length; i++) {
                    nextLogEntry[i] = lastApplied + 1;
                    matchIndex[i] = lastApplied;
                }
                while (true) {
                    if (System.currentTimeMillis() > time + 5 * 1000) {
                        time = System.currentTimeMillis();
                        for (int i = 0; i < servers.size(); i++) {
                            if (i != myReplicaID) {
                                Log[] logs = new Log[lastApplied - nextLogEntry[i] + 1];
                                for (int j = lastApplied - 1, k = logs.length - 1; j >= lastApplied - logs.length; j--) {
                                    logs[k] = log.get(j);
                                }
                                AppendEntry aps = new AppendEntry(currentTerm, myReplicaID, matchIndex[i], log.get(log.size() - 1).term, commitIndex, logs);
                                invoke(i, new Message("AppendEntry", aps, myReplicaID));
                            }
                        }
                    }
                    if (main_queue.size() > 0) {
                        Message m = main_queue.peek();
                        if (m.label.equals("AppendEntryReply")) {
                            AppendEntryReply ape = (AppendEntryReply) m.data;
                            if(ape.term == currentTerm) {
                                if (lastApplied >= nextLogEntry[m.senderID] && ape.success) {
                                    nextLogEntry[m.senderID] = lastApplied + 1;
                                    matchIndex[m.senderID] = lastApplied;
                                } else if (!ape.success) {
                                    nextLogEntry[m.senderID]--;
                                    matchIndex[m.senderID]--;
                                }
                                if (commitIndex < lastApplied) {
                                    int N = commitIndex + 1, maj = 0;
                                    for (int i = 0; i < matchIndex.length; i++) {
                                        if (matchIndex[i] >= N) maj++;
                                    }
                                    if (maj > (servers.size() / 2.0) && log.get(N).term == currentTerm) commitIndex = N;
                                }
                            }
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm, false);
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                votedFor = -1;
                                state = 2;
                                break;
                            }
                            if (pac.term <= currentTerm) pacReply.voteGranted = false;
                            System.out.println(m.senderID + " " + m.label + " " + pacReply.voteGranted + " " + lastApplied);
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID));
                        } else if (m.label.equals("ClientRequest")) {
                            String command = (String) m.data;
                            log.add(new Log(currentTerm, command));
                            lastApplied++;
                            nextLogEntry[myReplicaID]++;
                            matchIndex[myReplicaID]++;
                            System.out.println("Cliente " + m.label + " " + command + " " + lastApplied);
                        }
                        main_queue.remove();
                    }
                }
            } else if (state == 1) {//candidato
                currentTerm++;
                Boolean[] votos = new Boolean[servers.size()];
                votos[myReplicaID] = true;
                votedFor = myReplicaID;
                RequestVote rv = new RequestVote(currentTerm, myReplicaID, lastApplied, log.get(log.size() - 1).term);
                long initial_time = System.currentTimeMillis();
                System.out.println("Sou candidato");
                majorityInvoke(new Message("RequestVote", rv, myReplicaID));
                while (System.currentTimeMillis() <= initial_time + max_time * 1000) {
                    while (main_queue.size() != 0 && System.currentTimeMillis() <= initial_time + max_time * 1000) {
                        Message m = main_queue.peek();
                        //if (System.currentTimeMillis() >= initial_time + max_time * 1000) break;
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
                                    System.out.println("Sou o leader");
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
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm, false);
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                votedFor = -1;
                                state = 2;
                                initial_time = 0;
                                break;
                            }
                            if (pac.term <= currentTerm) pacReply.voteGranted = false;
                            System.out.println(
                                    "SenderID : " + m.senderID + "\n" +
                                    "Label :" + m.label  + "\n" +
                                    "Resposta :" + pacReply.voteGranted  + "\n" +
                                    "Pac Term :"  + pac.term  + "\n" +
                                    "Log lastApplied :" + lastApplied  + "\n" +
                                    "Log length :" + log.size() + "\n" +
                                    "------------------------------------------------------");
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID));
                        }
                        else if (m.label.equals("ClientRequest")) {
                            thread_queue[leader].add(m);
                        }
                        main_queue.remove();
                    }
                }
            } else if (state == 2) {//seguidor
                long initial_time = System.currentTimeMillis();
                boolean candidatura = true;
                while (System.currentTimeMillis() <= initial_time + max_time * 1000) {
                    while (main_queue.size() != 0 && System.currentTimeMillis() <= initial_time + max_time * 1000) {
                        Message m = main_queue.peek();
                        if (m.label.equals("AppendEntry")) {
                            initial_time = System.currentTimeMillis();
                            AppendEntry pac = (AppendEntry) m.data;
                            AppendEntryReply pacReply = new AppendEntryReply(currentTerm, false);
                            leader = m.senderID;
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                votedFor = -1;
                                state = 2;
                                candidatura = false;
                                initial_time = 0;
                                break;
                            }
                            if (pac.term < currentTerm) pacReply.success = false;
                            else if (lastApplied < pac.prevLogIndex) pacReply.success = false;
                            else if (lastApplied == pac.prevLogIndex) {//duvida
                                if (pac.leaderCommit > commitIndex) commitIndex = pac.leaderCommit < lastApplied ? pac.leaderCommit : lastApplied;
                                if (pac.entries != null) {
                                    for (int i = 0; i < pac.entries.length; i++) {
                                        log.add(pac.entries[i]);
                                        lastApplied++;
                                    }
                                }
                                pacReply.success = true;
                            } else if (lastApplied > pac.prevLogIndex) {
                                if (pac.entries != null) {
                                    for (int i = lastApplied; i > pac.prevLogIndex; i--) {
                                        this.log.remove(this.log.size() - 1);
                                        lastApplied--;
                                    }
                                    for (int i = 0; i < pac.entries.length; i++) {
                                        log.add(pac.entries[i]);
                                        lastApplied++;
                                    }
                                }
                                pacReply.success = true;
                            }
                            System.out.println(
                                    "SenderID : " + m.senderID + "\n" +
                                    "Label :" + m.label  + "\n" +
                                    "Resposta :" + pacReply.success  + "\n" +
                                    "Pac Term :"  + pac.term  + "\n" +
                                    "Pac LeaderCommit :"  + pac.leaderCommit  + "\n" +
                                    "Pac prevLogIndex :" +  pac.prevLogIndex  + "\n" +
                                    "Pac log length :" + pac.entries.length  + "\n" +
                                    "Log lastApplied :" + lastApplied  + "\n" +
                                    "Log length :" + log.size() + "\n" +
                                    "CommitIndex :" + commitIndex + "\n" +
                                    "------------------------------------------------------");
                            invoke(m.senderID, new Message<AppendEntryReply>("AppendEntryReply", pacReply, myReplicaID));
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm, false);
                            if (pac.term > currentTerm) {
                                currentTerm = pac.term;
                                votedFor = -1;
                                state = 2;
                                candidatura = false;
                                initial_time = 0;//
                                break;
                            }
                            if (pac.term < currentTerm) pacReply.voteGranted = false;
                            else {
                                if (votedFor == -1 && pac.term == currentTerm) { //&& lastApplied <= pac.lastLogIndex) {
                                    votedFor = pac.candidateId;
                                    pacReply.voteGranted = true;
                                } else pacReply.voteGranted = false;
                            }
                            System.out.println(
                                    "SenderID : " + m.senderID + "\n" +
                                    "Label :" + m.label  + "\n" +
                                    "Resposta :" + pacReply.voteGranted  + "\n" +
                                    "Pac Term :"  + pac.term  + "\n" +
                                    "Log lastApplied :" + lastApplied  + "\n" +
                                    "Log length :" + log.size() + "\n" +
                                    "------------------------------------------------------");
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID));
                        }
                        else if (m.label.equals("ClientRequest")) {
                            thread_queue[leader].add(m);
                        }
                        main_queue.remove();
                    }
                    if (candidatura && System.currentTimeMillis() > initial_time + max_time * 1000) state = 1;
                }
            }
        }
    }
}
/*registerHandler_Invoke
while(true){
    while(main_queue.size() != 0){
        Message m = main_queue.remove();
        ProcessRequest proc = requestHandlers.get(m.label);
        proc.exe(m);
    }
}
*/