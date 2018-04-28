/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

/**
 *
 * @author ilia-mint
 */
public class NetworkTcpClientApplication {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            if(args[1].equals("server")) {
                ServerRunner server = new ServerRunner(SocketServer.createServer(args[1], Integer.parseInt(args[2])));
                new Thread(server).start();
            } else if(args[1].equals("server")) {
                ClientRunner client = new ClientRunner(NetworkClientApiManager.getInstance().getClient(args[1], Integer.parseInt(args[2])));
                new Thread(client).start();
            }
            
        } catch(Exception ex) {
            System.out.print(ex.getMessage()); 
        }        
    }
    
}

class ServerRunner implements Runnable{
      
    SocketServer server;
    ServerRunner(SocketServer server){
       this.server=server; 
    }
    
    public void run() {
        try{
            server.Run();
        } catch(Exception ex) {
            System.out.print(ex.getMessage()); 
        }
    }
}

class ClientRunner implements Runnable{
      
    NetworkClientApi client;
    ClientRunner(NetworkClientApi client){
       this.client=client; 
    }
    
    public void run() {
        try{
            String tokenID = client.createNetwork("NETWORK_TOKEN");
            boolean result = client.registerNetwork(tokenID);
            System.out.println("Registering is "+(result ? "successful" : "failed"));
        } catch(Exception ex) {
            System.out.print(ex.getMessage()); 
        }
    }
}
