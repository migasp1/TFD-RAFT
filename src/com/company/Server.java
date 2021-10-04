package com.company;

import org.w3c.dom.ls.LSOutput;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    public List<String> servers;
    public String me;
    public int myReplicaID;
    public Map<String, ProcessRequest> requestHandlers;
    public ThreadConnect [] senders,receivers;


    public Server(String file, int replicaID){
        this.myReplicaID = replicaID;
        this.servers = new ArrayList<>();
        this.requestHandlers = new HashMap<>();
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
        senders = new ThreadConnect[servers.size() - 1];
        receivers = new ThreadConnect[servers.size() - 1];
    }

    public void create_connection(){
        try{
            for (int i = 0; i < servers.size(); i++) {
                senders[i] = new ThreadConnect();
                senders[i].setDaemon(true);
                senders[i].start();
            }
        }
        catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }

}