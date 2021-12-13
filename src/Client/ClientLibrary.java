package Client;

import Server.Message;
import Server.RPC.ClientRequest;
import Server.RPC.ClientRequestReply;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientLibrary {

    ArrayList<String> servers;
    String ip;
    int port, leader;
    volatile int seq_num;
    ConcurrentLinkedQueue<ClientRequestReply> queue;


    public ClientLibrary(String file, String ip, int port) {
        this.servers = new ArrayList<>();
        this.ip = ip;
        this.port = port;
        this.seq_num = 0;
        this.queue = new ConcurrentLinkedQueue<>();
        try {
            Scanner reader = new Scanner(new File(file));
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                this.servers.add(line);
            }
            reader.close();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        this.leader = new Random().nextInt(servers.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serv = new ServerSocket(port);
                    while(true) {
                        Socket soc = serv.accept();
                        ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());

                        ClientRequestReply crr = (ClientRequestReply) (((Message) inp.readObject()).data);

                        queue.add(crr);

                        out.close();
                        inp.close();
                        soc.close();
                    }
                    //serv.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public byte [] request(byte[] byteArray) throws Exception {
        boolean send = false, done = false;
        byte[] result = null;
        seq_num++;
        while(!done) {
            while (!send) {
                try {
                    String[] ip = servers.get(leader).split(":");
                    Socket soc = new Socket(ip[0], Integer.parseInt(ip[1]));
                    ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
                    ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
                    out.writeObject(new Message<ClientRequest>("ClientRequest", new ClientRequest(this.ip, port, seq_num, byteArray)));
                    out.close();
                    inp.close();
                    soc.close();
                    send = true;
                } catch (Exception ex) {
                    send = false;
                    int a = new Random().nextInt(servers.size());
                    while(a == leader)a = new Random().nextInt(servers.size());
                    leader = a;
                }
            }

            int timeout = 5000;
            long time = System.currentTimeMillis();
            while (true) {
                if (queue.peek() != null) {
                    time = System.currentTimeMillis();
                    ClientRequestReply r = queue.remove();
                    if(r.seq >= seq_num) {
                        result = r.data;
                        break;
                    }
                } else {
                    Thread.sleep(100);
                }
                if(System.currentTimeMillis() > time + timeout){
                    result = "timeout".getBytes(StandardCharsets.UTF_8);
                    break;
                }
            }
            String r = new String(result, StandardCharsets.UTF_8);
            if (r.equals("nao_ha_lider")) {
                send = false;
                leader = new Random().nextInt(servers.size());
                Thread.sleep(1000);
            } else if (r.contains("leader")) {
                leader = Integer.parseInt(r.split(":")[1]);
                send = false;
            }
            else if(r.equals("timeout")){
                send = false;
                //leader = new Random().nextInt(servers.size());
            }
            else {
                done = true;
            }
        }
        return result;
    }
}