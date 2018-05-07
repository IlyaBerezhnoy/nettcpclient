package com.netcracker.mesh_router.ui.networks.client;
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
    
    private class NetworkTCPClient implements NetworkClientApi {
    
        private SocketChannel client = null;        
        private final TlvBox tlvBox = new TlvBox();
        
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        private Map<Long, LinkedList<Tlv>> packetPool = new HashMap<>();
        private Long incomingReqId = null;
        private final Lock lock = new ReentrantLock(); 
        private final Condition getNewPacketCond = lock.newCondition();        
        //private final Object lock = new Object();
                  
        public NetworkTCPClient() throws IOException {
            
        }
        
        public void connect(String hostName, int portNumber) throws IOException {
            lock.lock(); 
             try {
                 if(client != null && client.isConnected())
                    client.close(); 
                buffer.clear();
                client = SocketChannel.open(new InetSocketAddress(hostName, portNumber));                
            } catch(IOException e) {
                throw e;
            } finally {
                 lock.unlock(); 
            }
        }
                                        
        @Override
        public String createNetwork(String clientToken) throws IOException, IllegalArgumentException, InterruptedException {
            
            String response = null;                       
            if(clientToken != null) {
                try{
                    List<Tlv> inArr = sendParam(new Tlv(TlvType.NET_TOKEN, clientToken.getBytes()));
                    if(inArr != null){
                        if(inArr.get(1).getType() != TlvType.OVERLAY_ID.getVal())
                            throw new IllegalArgumentException("Type of received data does not correspond overlay id");
                        
                        response = new String(inArr.get(1).getValue()).trim();
                    }
                    /*
                    if(inReqId != curReqId) {
                        synchronized(lock){
                            packetPool.put(inReqId, tlvArr);
                            tlvArr = null;
                        }
                        
                        for(Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()){
                            if(entry.getKey().getId() == inReqId){
                                entry.getKey().start();
                                break;
                            }
                        }
                        

                        try{
                            Thread.sleep(100);
                            if(packetPool.containsKey(curReqId)){
                                tlvArr = packetPool.remove(curReqId);                                    
                                response = new String(tlvArr.get(1).getValue()).trim();                
                            }                                    
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    */                      
                } catch(InterruptedException ex) {
                    throw ex;
                }
            }
            return response;                            
        }
    
        @Override
        public boolean registerNetwork(String overlayID) throws IOException, IllegalArgumentException, InterruptedException {
            
            if(overlayID != null) {
                try {

                    List<Tlv> tlvArr = sendParam(new Tlv(TlvType.OVERLAY_ID, overlayID.getBytes()));
                    if(tlvArr != null) {
                        if(tlvArr.get(1).getLength() != Byte.BYTES 
                                || tlvArr.get(1).getValue()[0] > 2)
                            throw new IllegalArgumentException("Type of received data is invalid");

                        byte param = tlvArr.get(1).getValue()[0]; 
                        return (param == 1);
                    }
                
                } catch(InterruptedException ex) {
                    throw ex;
                }
            }
            return false;
        } 
        
        private List<Tlv> sendParam(Tlv tlvParam) throws IOException, IllegalArgumentException, InterruptedException {
        
            lock.lock(); 
            LinkedList<Tlv> response = null;
            try{ 
                
                List<Tlv> tlvPacket= new LinkedList<>();
                Long curReqId = Thread.currentThread().getId();
                tlvPacket.add(tlvBox.putLong2Tlv(TlvType.REQUEST_ID.getVal(), curReqId));
                tlvPacket.add(tlvParam);
                   
                buffer.clear(); 
                buffer.put( tlvBox.serialize(tlvPacket) );
                buffer.flip();
                client.write(buffer);
                buffer.clear();  
                int readBytes = client.read(buffer);                
                LinkedList<Tlv> tlvArr = tlvBox.parse(buffer.array(), 0, readBytes);
                    
                if( tlvArr.size() != 2 
                    || tlvArr.get(0).getType() != TlvType.REQUEST_ID.getVal()
                        )
                    throw new IllegalArgumentException("Type of received data does not correspond request id");
                    
                    Long recReqId = tlvBox.getLongFromTlv(tlvArr.get(0));
                     
                    if(recReqId != curReqId) {
                        packetPool.put(recReqId, tlvArr);
                        tlvArr = null;
                        incomingReqId = recReqId;
                        getNewPacketCond.signalAll();
                        
                        while(incomingReqId != curReqId) { 
                            getNewPacketCond.await();
                        }  
                        response = packetPool.remove(curReqId);
                    } else {
                        response = tlvArr;
                    }                                               
                    
            } catch(InterruptedException ex) {
                throw ex;
            } finally {
                lock.unlock();
            }
            return response;  
        }
    }
           
    private static NetworkClientApiManager mInstance;
    private static final Object lock = new Object();
    private NetworkTCPClient mClient;
        
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
        
//        synchronized(lock) {
//            if(mClient == null)
//                throw new RuntimeException("Network client is not initialized");
//        }
        return mClient;
    }
    
    public void initClient(String hostName, int portNumber) throws IOException {
        synchronized(lock) {
            if(mClient == null) {
                NetworkTCPClient tcpClient = new NetworkTCPClient();
                tcpClient.connect(hostName, portNumber);
                mClient = tcpClient; 
            } else {                
                mClient.connect(hostName, portNumber);
            }          
        }
    }    
}
