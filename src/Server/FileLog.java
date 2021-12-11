package Server;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/*
term
numSeq
logs
 */

public class FileLog {


    //public ArrayList<Log> logs;
    public int [] term;//, seq;
    public String name;
    int top;

    public FileLog(int replica, int [] term){//, ArrayList<Log> logs){
        File f = new File("RaftLogs"+ replica +".txt");
        //this.logs = logs;
        this.term = term;
        //this.seq = seq;
        this.name = f.getAbsolutePath();
        this.top = 0;
        if(!f.exists()) {
            try {
                f.createNewFile();
                FileWriter fw = new FileWriter(this.name);
                fw.append(term[0] + "\n");
                /*for(Log l:logs){
                    fw.append(l.toString() + "\n");
                    top++;
                }*/
                fw.append("1;initial_command" + "\n");
                top++;

                fw.flush();
                fw.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void act_term(){
        Scanner sc = null;
        StringBuffer buffer = null;
        try {
            sc = new Scanner(new File(this.name));
            buffer = new StringBuffer();
            String a = sc.nextLine();
            buffer.append(term[0] + System.lineSeparator());
            //buffer.append(seq[0] + System.lineSeparator());
            int i = 0;
            while (sc.hasNextLine()) {
                buffer.append(sc.nextLine()+System.lineSeparator());
                i++;
            }
            if(i != top) System.err.println("logs nao sync");
            sc.close();
            FileWriter fw = new FileWriter(this.name);
            fw.write(buffer.toString());
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addLog(Log l){
        try {
            FileWriter fw = new FileWriter(this.name, true);
            fw.append(l.toString() + "\n");
            fw.flush();
            fw.close();
            //logs.add(l);
            top++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Log readLog(int i){
        if(i > top)return null;
        Scanner fr = null;
        try {
            fr = new Scanner(new File(this.name));
            this.term[0] = Integer.parseInt(fr.next());
            //this.seq[0] = Integer.parseInt(fr.next());
            int j = 0;
            while (fr.hasNextLine()) {
                String data = fr.nextLine();
                if(j == i)return new Log(Integer.parseInt(data.split(";")[0]), data.split(";")[1]);
                j++;
            }
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeLog(){
        if(top < 0)return;
        Scanner sc = null;
        try {
            sc = new Scanner(new File(this.name));
            StringBuffer buffer = new StringBuffer();
            buffer.append(sc.nextLine() + System.lineSeparator());
            //buffer.append(seq[0] + System.lineSeparator());
            int i = 0;
            while (sc.hasNextLine() && i < top - 1) {
                buffer.append(sc.nextLine()+System.lineSeparator());
                i++;
            }
            sc.close();
            FileWriter fw = new FileWriter(this.name);
            fw.write(buffer.toString());
            fw.flush();
            fw.close();
            top--;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
