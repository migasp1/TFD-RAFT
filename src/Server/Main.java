package Server;

public class Main {

    public static void main(String[] args) {
        Server ser = new Server(args[0], Integer.parseInt(args[1]));
        /*ser.registerHandler("ClientRequest", new ProcessRequest() {
            @Override
            public void exe(Message m) {
                if(ser.myReplicaID == 0){
                    m.label = "AppendEntrie";
                    m.senderID = ser.myReplicaID;
                    ser.invoke(1, m);
                }
            }
        });
        ser.registerHandler("AppendEntrie", new ProcessRequest() {
            @Override
            public void exe(Message m) {
                if(ser.myReplicaID != 0){
                    ser.invoke(m.senderID, new Message("AppendEntrieReply", "adeus", ser.myReplicaID));
                }
            }
        });
        ser.registerHandler("AppendEntrieReply", new ProcessRequest() {
            @Override
            public void exe(Message m) {
                if(ser.myReplicaID == 0){
                    System.out.println((String)m.data);
                }
            }
        });*/
        ser.execute();

    }
}
