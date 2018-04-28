package com.netcracker.mesh_router.ui.networks.client;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static sun.misc.GThreadHelper.lock;
import java.util.concurrent.locks.*;

public class NetworkClientApiManager {
    
    private class NetworkTCPClient implements NetworkClientApi {
    
        private SocketChannel client;        
        private TlvBox tlvBox;
        
        private Map<Long, List<Tlv>> packetPool = new HashMap<>();
        private Long incomingReqId = null;
        private final Lock lock = new ReentrantLock(); 
        private final Condition getNewPacketCond = lock.newCondition();        
        //private final Object lock = new Object();
                  
        public NetworkTCPClient() throws IOException {
            tlvBox = new TlvBox();             
        }
        
        public void start(String hostName, int portNumber) throws IOException {
             try {
                client = SocketChannel.open(new InetSocketAddress(hostName, portNumber));                
            } catch(IOException e) {
                //TODO LOG
                throw e;
            }
        }
        
        public void stop() throws IOException {
            client.close();            
        }
                        
        @Override
        public String createNetwork(String clientToken) throws IOException, IllegalArgumentException {
            
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
                    Thread.currentThread().interrupt();
                }
            }
            return response;                            
        }
    
        @Override
        public boolean registerNetwork(String overlayID) throws IOException, IllegalArgumentException {
            
            if(overlayID != null) {
                try {

                    List<Tlv> tlvArr = sendParam(new Tlv(TlvType.OVERLAY_ID, overlayID.getBytes()));
                    if(tlvArr != null) {
                        if(tlvArr.get(1).getLength() != Byte.BYTES 
                                || tlvArr.get(0).getValue()[0] > 2)
                            throw new IllegalArgumentException("Type of received data is invalid");

                        byte param = tlvArr.get(0).getValue()[0]; 
                        return (param == 1);
                    }
                
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return false;
        } 
        
        private List<Tlv> sendParam(Tlv tlvParam) throws IOException, IllegalArgumentException, InterruptedException {
        
            lock.lock();            
            try{                
                List<Tlv> tlvPacket= new LinkedList<>();
                Long curReqId = Thread.currentThread().getId();
                tlvPacket.add(tlvBox.putLong2Tlv(TlvType.REQUEST_ID.getVal(), curReqId));
                tlvPacket.add(tlvParam);
                    
                ByteBuffer buffer = ByteBuffer.wrap( tlvBox.serialize(tlvPacket) );
                client.write(buffer);
                buffer.clear();  
                int readBytes = client.read(buffer);
                List<Tlv> tlvArr = tlvBox.parse(buffer.array(), 0, readBytes);
                    
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
                    }                                                
                    return packetPool.remove(curReqId);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
            return null;  
        }
    }
    
    private NetworkClientApiManager mInstance;
    private NetworkClientApi mClient;
        
    public NetworkClientApiManager() {         
    }
    
    private NetworkClientApiManager getInstance(){
        if(mInstance == null) {
            mInstance = new NetworkClientApiManager();
        }
        return mInstance;
    }
    
    private NetworkClientApi getClient(String hostName, int portNumber) throws IOException {
        
        if(mClient == null) {
            NetworkTCPClient tcpClient = new NetworkTCPClient();
            tcpClient.start(hostName, portNumber);
            mClient = tcpClient;
        }
        
        return mClient;
    }
    
    
}
