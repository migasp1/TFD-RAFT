package Client;

import Server.Message;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class ClientLibrary {

    ArrayList<String> servers;

    public ClientLibrary(String file) {
        this.servers = new ArrayList<>();
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
    }

    public void request(byte[] byteArray) throws IOException {
        int i = new Random().nextInt(servers.size());
        String[] ip = servers.get(i).split(":");
        Socket soc = new Socket(ip[0], Integer.parseInt(ip[1]));
        ObjectOutputStream out = new ObjectOutputStream(soc.getOutputStream());
        ObjectInputStream inp = new ObjectInputStream(soc.getInputStream());
        String str = new String(byteArray, StandardCharsets.UTF_8);
        out.writeObject(new Message<String>("ClientRequest", str));

        out.close();
        inp.close();
        soc.close();
    }
}