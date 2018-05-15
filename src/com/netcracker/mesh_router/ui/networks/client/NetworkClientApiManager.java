package com.netcracker.mesh_router.ui.networks.client;
import com.netcracker.mesh_router.ui.networks.client.tlv.TlvType;
import com.netcracker.mesh_router.ui.networks.client.tlv.TlvBox;
import com.netcracker.mesh_router.ui.networks.client.tlv.Tlv;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.*;

public class NetworkClientApiManager {
        
    private static NetworkClientApiManager mInstance;
    private static final Object lock = new Object();
    private NetworkTcpClient mClient;
        
    private NetworkClientApiManager() {         
    }
    
    public static NetworkClientApiManager getInstance(){
        synchronized(lock){
            if(mInstance == null) {
                mInstance = new NetworkClientApiManager();          
            }
        }
        return mInstance;
    }
    
    public NetworkClientApi getClient() {        
        synchronized(lock) {
            if(mClient == null)
                throw new RuntimeException("Network channel is not initialized");
            return mClient;
        }
        
    }
    
    public void initClient(String hostName, int portNumber) throws IOException {
        synchronized(lock) {
            if(mClient == null) {
                NetworkTcpClient tcpClient = new NetworkRpcClient();
                tcpClient.connect(hostName, portNumber);
                mClient = tcpClient; 
            } else {                
                mClient.connect(hostName, portNumber);
            }          
        }
    }  
    
    public void closeClient() {
        synchronized(lock) {
            try{
                if(mClient != null) 
                    mClient.close();
            } catch(Exception ex){}        
        }
    }
    
    public boolean isClientConnected() {
        synchronized(lock) {
            return (mClient != null) && (mClient.isConnected());
        }
    }
}
