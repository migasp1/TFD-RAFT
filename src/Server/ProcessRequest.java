package Server;

import Server.RPC.ClientRequest;
import Server.RPC.ClientRequestReply;

public interface ProcessRequest {
    public byte [] exe(Log req);
}
