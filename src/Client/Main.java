package Client;


import java.nio.charset.StandardCharsets;

public class Main{
    public static void main(String[] args) throws Exception{
        ClientLibrary cLib = new ClientLibrary(args[0], args[1], Integer.parseInt(args[2]));

        for (int i = 0; i < 100000;) {
            byte [] r = cLib.request((i + "").getBytes(StandardCharsets.UTF_8));
            i = Integer.parseInt(new String(r, StandardCharsets.UTF_8));
            System.out.println(i);
            Thread.sleep(1000);
        }
    }
}