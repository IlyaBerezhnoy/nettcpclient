/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

import com.netcracker.mesh_router.ui.networks.client.rpc.NetworkRpcServer;
import com.netcracker.mesh_router.ui.networks.client.tlv.NetworkTlvServer;

/**
 *
 * @author ilia-mint
 */
public class NetworkRegistrationService {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try{
            
            if(args.length < 3)
                throw new Exception("wrong number of arguments");
            
            if(args[0].equals("server")) {
                ServerRunner server = new ServerRunner(NetworkRpcServer.createServer(args[1], Integer.parseInt(args[2])));
                for(int i=0;i< Integer.parseInt(args[3]);i++){                     
                    new Thread(server).start();                
                }
            } else if(args[0].equals("client")) {
                NetworkClientApiManager.getInstance().initClient(args[1], Integer.parseInt(args[2]));
                ClientRunner client = new ClientRunner(NetworkClientApiManager.getInstance().getClient());
                for(int i=0;i<Integer.parseInt(args[3]);i++){                                        
                    new Thread(client).start();                
                }
            }
            
        } catch(Exception ex) {
            System.out.println(ex.getMessage()); 
        }        
    }
    
}

class ServerRunner implements Runnable{
      
    NetworkRpcServer server;
    ServerRunner(NetworkRpcServer server){
       this.server=server; 
    }
    
    public void run() {
        try{            
            server.Run();            
        } catch(Exception ex) {
            System.out.println(ex.getMessage()); 
        }
    }
}

class ClientRunner implements Runnable{
      
    NetworkClientApi client;
    ClientRunner(NetworkClientApi client) {
       this.client = client; 
    }
    
    public void run() {
        try{
            String tokenID = client.createNetwork("NETWORK_TOKEN"+Thread.currentThread().getId());
            System.out.println("Thread "+Thread.currentThread().getId()+". tokenID: "+tokenID);
            Thread.yield();
            boolean result = client.registerNetwork(tokenID);
            System.out.println("Thread "+Thread.currentThread().getId()+". Registering is "+(result ? "successful" : "failed"));    
           
        } catch(Exception ex) {
            System.out.println(ex.getMessage()); 
        }
    }
}
