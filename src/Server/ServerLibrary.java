package Server;

import Server.RPC.*;
import Server.Threads.Thread_Client_Receiver;
import Server.Threads.Thread_Client_Sender;
import Server.Threads.Thread_Connect_Receiver;
import Server.Threads.Thread_Connect_Sender;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerLibrary {

    public int [] currentTerm, messSeqNumber;

    public int votedFor, commitIndex, lastApplied;
    //public ArrayList<Log> log;

    public int[] nextLogEntry, matchIndex;

    int leader;
    long max_time;

    public List<String> servers;

    public String me;
    public int myReplicaID, state;

    public Thread_Connect_Sender[] senders;
    public Thread_Connect_Receiver[] receivers;

    public RAFTMessagePriorityBlockingQueue main_queue, client_response_queue;
    public RAFTMessagePriorityBlockingQueue[] thread_queue;

    public Thread_Client_Receiver client_receiver;
    public Thread_Client_Sender client_sender;

    public AtomicBoolean[] alive;

    FileLog file;

    ProcessRequest proc;

    public ServerLibrary(String file, int myReplicaID){
        this(file, myReplicaID, 0, 0);
    }

    public ServerLibrary(String file, int replicaID, int currentTerm, int lastLogIndex) {
        this.myReplicaID = replicaID;
        this.state = 2;
        this.servers = new ArrayList<>();
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
        this.client_response_queue = new RAFTMessagePriorityBlockingQueue();
        this.alive = new AtomicBoolean[servers.size()];
        this.leader = 0;
        this.currentTerm = new int[]{currentTerm};
        this.messSeqNumber = new int[]{0};
        this.votedFor = -1;
        //this.log = new ArrayList();
        this.file = new FileLog(myReplicaID, this.currentTerm);//, this.log);
        /*if(this.log.size() == 0){
            for (int i = 0; i < lastLogIndex + 1; i++) {
                this.file.addLog(new Log(this.currentTerm[0], "initial_command"));
            }
        }*/
        this.lastApplied = this.file.top;//this.log.size() - 1;
        this.commitIndex = this.lastApplied;
        this.nextLogEntry = new int[servers.size()];
        this.matchIndex = new int[servers.size()];
        this.max_time = new Random().nextInt(11) + 5;
        //System.out.println(this.log.toString());
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
            this.client_sender = new Thread_Client_Sender(client_response_queue);
            this.client_sender.setDaemon(true);
            this.client_sender.start();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void invoke(int replicaId, Message m) {
        thread_queue[replicaId].add(m);
    }

    public void registerHandler(ProcessRequest proc) {
        this.proc = proc;
    }

    public void majorityInvoke(Message m) {
        for (int i = 0; i < thread_queue.length; i++) {
            if (i != myReplicaID) thread_queue[i].add(m);
        }
    }

    //aqui vai ser onde vamos defenir o algoritmo
    public void execute() {
        if(this.proc == null){System.err.println("Message is null. Must register an handler"); return;}
        while (true) {
            if (state == 0) {//leader
                long time = System.currentTimeMillis();
                AppendEntry ap = new AppendEntry(currentTerm[0], myReplicaID, lastApplied, file.readLog(file.top).term, commitIndex, new Log[0]);//log.get(log.size() - 1).term
                messSeqNumber[0]++;
                majorityInvoke(new Message("AppendEntry", ap, myReplicaID, messSeqNumber[0]));
                for (int i = 0; i < nextLogEntry.length; i++) {
                    nextLogEntry[i] = lastApplied + 1;
                    matchIndex[i] = lastApplied;
                }
                while (true) {
                    if (System.currentTimeMillis() > time + 4 * 1000) {
                        time = System.currentTimeMillis();
                        messSeqNumber[0]++;
                        for (int i = 0; i < servers.size(); i++) {
                            if (i != myReplicaID) {
                                Log[] logs = new Log[lastApplied - nextLogEntry[i] + 1];
                                for (int j = lastApplied, k = logs.length - 1; j >= lastApplied - logs.length + 1; j--, k--) {
                                    logs[k] = file.readLog(j);//log.get(j);
                                }
                                AppendEntry aps = new AppendEntry(currentTerm[0], myReplicaID, matchIndex[i], file.readLog(file.top).term, commitIndex, logs);//log.get(log.size() - 1).term
                                invoke(i, new Message("AppendEntry", aps, myReplicaID, messSeqNumber[0]));
                            }
                        }
                    }
                    Message m = main_queue.peek();
                    if(m != null) {
                        if (m.label.equals("AppendEntryReply")) {
                            AppendEntryReply ape = (AppendEntryReply) m.data;
                            if (ape.term > currentTerm[0]) {
                                currentTerm[0] = ape.term;
                                file.act_term();
                                votedFor = -1;
                                state = 2;
                                break;
                            }
                            if(ape.term == currentTerm[0] && messSeqNumber[0] == m.seqNumber) {
                                if (lastApplied >= nextLogEntry[m.senderID] && ape.success) {
                                    nextLogEntry[m.senderID] = lastApplied + 1;
                                    matchIndex[m.senderID] = lastApplied;
                                } else if (!ape.success && nextLogEntry[m.senderID] > 0 && matchIndex[m.senderID] > 0) {
                                    nextLogEntry[m.senderID]--;
                                    matchIndex[m.senderID]--;
                                }
                                if (commitIndex < lastApplied) {
                                    int N = commitIndex + 1, maj = 0;
                                    for (int i = 0; i < matchIndex.length; i++) {
                                        if (matchIndex[i] >= N) maj++;
                                    }
                                    if (maj > (servers.size() / 2.0) ){//&& file.readLog(file.top).term == currentTerm[0]){
                                        commitIndex = N;
                                    }
                                }
                            }
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm[0], false);
                            if (pac.term > currentTerm[0]) {
                                currentTerm[0] = pac.term;
                                file.act_term();
                                votedFor = -1;
                                state = 2;
                                break;
                            }
                            if (pac.term <= currentTerm[0]) pacReply.voteGranted = false;
                            System.out.println(m.senderID + " " + m.label + " " + pacReply.voteGranted + " " + lastApplied);
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID, m.seqNumber));
                        } else if (m.label.equals("ClientRequest")) {
                            ClientRequest command = (ClientRequest) m.data;
                            file.addLog(new Log(currentTerm[0], command.getData(), command.me_ip, command.me_port, command.seq));
                            lastApplied++;
                            nextLogEntry[myReplicaID]++;
                            matchIndex[myReplicaID]++;
                            Log ll = file.readLog(lastApplied);
                            byte [] result = proc.exe(ll);
                            client_response_queue.add(new Message<>("ClientRequestReply", new ClientRequestReply(ll.ip, ll.port, ll.seq, result)));
                        }
                        main_queue.remove();
                    }
                    else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (state == 1) {//candidato
                currentTerm[0]++;
                file.act_term();
                Boolean[] votos = new Boolean[servers.size()];
                votos[myReplicaID] = true;
                votedFor = myReplicaID;
                RequestVote rv = new RequestVote(currentTerm[0], myReplicaID, lastApplied, file.readLog(file.top).term);//log.get(log.size() - 1).term
                long initial_time = System.currentTimeMillis();
                System.out.println("Sou candidato");
                messSeqNumber[0]++;
                majorityInvoke(new Message("RequestVote", rv, myReplicaID, messSeqNumber[0]));
                while (System.currentTimeMillis() <= initial_time + max_time * 1000) {
                    Message m = main_queue.peek();
                    if(m != null) {
                        //if (System.currentTimeMillis() >= initial_time + max_time * 1000) break;
                        if (m.label.equals("RequestVoteReply")) {
                            RequestVoteReply pac = (RequestVoteReply) m.data;
                            if (pac.term > currentTerm[0]) {
                                currentTerm[0] = pac.term;
                                file.act_term();
                                state = 2;
                                initial_time = 0;
                            } else if (pac.term == currentTerm[0] && messSeqNumber[0] == m.seqNumber) {
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
                            if (pac.term > currentTerm[0]) {
                                currentTerm[0] = pac.term;
                                file.act_term();
                                state = 2;
                                initial_time = 0;
                            }
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm[0], false);
                            if (pac.term > currentTerm[0]) {
                                currentTerm[0] = pac.term;
                                file.act_term();
                                votedFor = -1;
                                state = 2;
                                initial_time = 0;
                                break;
                            }
                            if (pac.term <= currentTerm[0]) pacReply.voteGranted = false;
                            System.out.println(
                                    "SenderID : " + m.senderID + "\n" +
                                    "Label :" + m.label  + "\n" +
                                    "Resposta :" + pacReply.voteGranted  + "\n" +
                                    "Pac Term :"  + pac.term  + "\n" +
                                    "Log lastApplied :" + lastApplied  + "\n" +
                                    "Log length :" + (file.top + 1) + "\n" +
                                    "------------------------------------------------------");
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID, m.seqNumber));
                        }
                        else if (m.label.equals("ClientRequest")) {
                            ClientRequest command = (ClientRequest) m.data;
                            client_response_queue.add(new Message<>("ClientRequestReply", new ClientRequestReply(command.me_ip, command.me_port, command.seq + 1, "nao_ha_lider".getBytes(StandardCharsets.UTF_8))));
                        }
                        main_queue.remove();
                    }
                    else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (state == 2) {//seguidor
                long initial_time = System.currentTimeMillis();
                boolean candidatura = true;
                while (System.currentTimeMillis() <= initial_time + max_time * 1000) {
                    Message m = main_queue.peek();
                    if(m != null) {
                        if (m.label.equals("AppendEntry")) {
                            initial_time = System.currentTimeMillis();
                            AppendEntry pac = (AppendEntry) m.data;
                            AppendEntryReply pacReply = new AppendEntryReply(currentTerm[0], false);
                            leader = m.senderID;
                            if (pac.term > currentTerm[0]) {
                                currentTerm[0] = pac.term;
                                file.act_term();
                                votedFor = -1;
                                state = 2;
                                candidatura = false;
                                initial_time = 0;
                                break;
                            }
                            if (pac.term < currentTerm[0]) pacReply.success = false;
                            else if (lastApplied < pac.prevLogIndex) pacReply.success = false;
                            else if (lastApplied == pac.prevLogIndex) {//duvida
                                if (pac.leaderCommit > commitIndex) commitIndex = pac.leaderCommit < lastApplied ? pac.leaderCommit : lastApplied;
                                if (pac.entries != null) {
                                    for (int i = 0; i < pac.entries.length; i++) {
                                        file.addLog(pac.entries[i]);
                                        //log.add(pac.entries[i]);
                                        lastApplied++;
                                    }
                                }
                                pacReply.success = true;
                            } else if (lastApplied > pac.prevLogIndex) {
                                if (pac.entries != null) {
                                    for (int i = lastApplied; i > pac.prevLogIndex; i--) {
                                        this.file.removeLog();
                                        //log.remove(this.log.size() - 1);
                                        lastApplied--;
                                    }
                                    for (int i = 0; i < pac.entries.length; i++) {
                                        file.addLog(pac.entries[i]);
                                        //log.add(pac.entries[i]);
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
                                    "Log length :" + (file.top + 1) + "\n" +
                                    "CommitIndex :" + commitIndex + "\n" +
                                    "------------------------------------------------------");
                            invoke(m.senderID, new Message<AppendEntryReply>("AppendEntryReply", pacReply, myReplicaID, m.seqNumber));
                        } else if (m.label.equals("RequestVote")) {
                            RequestVote pac = (RequestVote) m.data;
                            RequestVoteReply pacReply = new RequestVoteReply(currentTerm[0], false);
                            if (pac.term > currentTerm[0]) {
                                currentTerm[0] = pac.term;
                                file.act_term();
                                votedFor = -1;
                                state = 2;
                                candidatura = false;
                                initial_time = 0;//
                                break;
                            }
                            if (pac.term < currentTerm[0]) pacReply.voteGranted = false;
                            else {
                                if (votedFor == -1 && pac.term == currentTerm[0]) { //&& lastApplied <= pac.lastLogIndex) {
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
                                    "Log length :" + (file.top + 1) + "\n" +
                                    "------------------------------------------------------");
                            invoke(m.senderID, new Message<RequestVoteReply>("RequestVoteReply", pacReply, myReplicaID, m.seqNumber));
                        }
                        else if (m.label.equals("ClientRequest")) {
                            ClientRequest command = (ClientRequest) m.data;
                            client_response_queue.add(new Message<>("ClientRequestReply", new ClientRequestReply(command.me_ip, command.me_port, command.seq, ("leader:" + leader).getBytes(StandardCharsets.UTF_8))));
                        }
                        main_queue.remove();
                    }
                    else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (candidatura && System.currentTimeMillis() > initial_time + max_time * 1000) state = 1;
                }
            }
        }
    }
}
