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
        
        private Map<Long, List<Tlv>> regPacketPool = new HashMap<>();
        private Long incomingCreId = null;
        private final Lock lock = new ReentrantLock(); 
        private final Condition getNewOverlayPacketCond = lock.newCondition();        
        //private final Object lock = new Object();
                
        private Map<Long, List<Tlv>> crePacketPool = new HashMap<>();
        private Long incomingRegId = null;
        private final Lock lock = new ReentrantLock(); 
        private final Condition getNewOverlayPacketCond = lock.newCondition();        
        
        
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
            lock.lock();
            try {                
                if(clientToken != null) {

                    List<Tlv> tlvPacket= new LinkedList<>();
                    Long curReqId = Thread.currentThread().getId();
                    tlvPacket.add(tlvBox.putLong2Tlv(TlvType.REQUEST_ID.getVal(), curReqId));
                    tlvPacket.add(new Tlv(TlvType.NET_TOKEN, clientToken.getBytes()));

                    ByteBuffer buffer = ByteBuffer.wrap(tlvBox.serialize(tlvPacket));
                    int readBytes = sendBuffer(buffer);                
                    List<Tlv> tlvArr = tlvBox.parse(buffer.array(), 0, readBytes);

                    if( tlvArr.size() != 2 
                            || tlvArr.get(0).getType() != TlvType.REQUEST_ID.getVal()
                            || tlvArr.get(1).getType() != TlvType.OVERLAY_ID.getVal()
                            )
                        throw new IllegalArgumentException("type of data received does not correspond overlay id");                                           

                    Long inReqId = tlvBox.getLongFromTlv(tlvArr.get(0));
                    if(inReqId != curReqId) {                    
                        packetPool.put(inReqId, tlvArr);
                        tlvArr = null;
                        incomingCreId = inReqId;
                        getNewPacketCond.signalAll();
                    }
                    while(incomingCreId != curReqId) { 
                        getNewPacketCond.await();
                    }          
                    tlvArr = packetPool.remove(curReqId);                                    
                    response = new String(tlvArr.get(1).getValue()).trim();
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
                }        
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }finally {
                lock.unlock();
            }
            return response;                            
        }
    
        @Override
        public boolean registerNetwork(String overlayID) throws IOException, IllegalArgumentException {
            
            if(overlayID != null) {
                
                buffer = ByteBuffer.wrap(
                        tlvBox.serialize(
                                new Tlv(TlvType.OVERLAY_ID, overlayID.getBytes())
                        )
                );
                int readBytes = sendBuffer(buffer);
                
                List<Tlv> tlvArr = tlvBox.parse(buffer.array(), 0, readBytes);
                do { 
                    if(tlvArr.size() == 0) break;      
                    
                    Tlv inTlv = tlvArr.get(0);                    
                    if(inTlv.getLength() != Byte.BYTES) break;
                    
                    byte param = tlvArr.get(0).getValue()[0];
                    if(param > 2)   break;
                    
                    return (param == 1);
                    
                } while(false);
                
                throw new IllegalArgumentException("type of data received is invalid");              
            }
            return false;
        } 
        
        private int sendBuffer(ByteBuffer buffer) throws IOException {
        
            client.write(buffer);
            buffer.clear();            
            return client.read(buffer);  
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
