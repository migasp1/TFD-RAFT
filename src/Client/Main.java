package Client;


import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Main{
    public static void main(String[] args) throws Exception{
        ClientLibrary cLib = new ClientLibrary("servers.txt");

        for (int i = 0; i < 10; i++) {
            cLib.request(("comando" + i).getBytes(StandardCharsets.UTF_8));
            Thread.sleep((new Random().nextInt(6) + 2) * 1000);
        }
    }
}