/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

import com.netcracker.mesh_router.ui.networks.client.tlv.Tlv;
import com.netcracker.mesh_router.ui.networks.client.tlv.TlvBox;
import com.netcracker.mesh_router.ui.networks.client.tlv.TlvType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;


class NetworkTlvClient extends NetworkTcpClient {
    
    private final Condition getNewPacketCond = lock.newCondition();                
    private final TlvBox tlvBox = new TlvBox();
    private Map<Long, LinkedList<Tlv>> packetPool = new HashMap<>();
                                        
    @Override
    public String createNetwork(String clientToken) throws IOException, IllegalArgumentException, InterruptedException {
            
            String response = null;                       
            if(clientToken != null) {               
                List<Tlv> inArr = sendParam(new Tlv(TlvType.NET_TOKEN, clientToken.getBytes()));
                if(inArr != null){
                    if(inArr.get(1).getType() != TlvType.OVERLAY_ID.getVal())
                        throw new IllegalArgumentException("Type of received data does not correspond overlay id");                        
                    response = new String(inArr.get(1).getValue()).trim();
                }
            }
            return response;                            
        }
    
        @Override
        public boolean registerNetwork(String overlayID) throws IOException, IllegalArgumentException, InterruptedException {
            
            if(overlayID != null) {               
                List<Tlv> tlvArr = sendParam(new Tlv(TlvType.OVERLAY_ID, overlayID.getBytes()));
                if(tlvArr != null) {
                    if(tlvArr.get(1).getLength() != Byte.BYTES 
                            || tlvArr.get(1).getValue()[0] > 2)
                        throw new IllegalArgumentException("Type of received data is invalid");

                    byte param = tlvArr.get(1).getValue()[0]; 
                    return (param == 1);
                }
            }
            return false;
        } 
        
        private List<Tlv> sendParam(Tlv tlvParam) throws IOException, IllegalArgumentException, InterruptedException {
                    
            LinkedList<Tlv> response = null;
            List<Tlv> tlvPacket= new LinkedList<>();
            Long curReqId = Thread.currentThread().getId();
            tlvPacket.add(tlvBox.putLong2Tlv(TlvType.REQUEST_ID.getVal(), curReqId));
            tlvPacket.add(tlvParam);
            
            ByteBuffer buffer = localBuffer.get();
            buffer.clear(); 
            buffer.put( tlvBox.serialize(tlvPacket) );
            buffer.flip();
            
            lock.lock();             
            try{ 
                channel.write(buffer);
                buffer.clear();
                int readBytes = channel.read(buffer);
                LinkedList<Tlv> tlvArr = tlvBox.parse(buffer.array(), 0, readBytes);
                    
                if( tlvArr.size() != 2 
                    || tlvArr.get(0).getType() != TlvType.REQUEST_ID.getVal()
                        )
                    throw new IllegalArgumentException("Type of received data does not correspond request id");
                    
                    Long recReqId = tlvBox.getLongFromTlv(tlvArr.get(0));                     
                    if(!recReqId.equals(curReqId)) {
                        packetPool.put(recReqId, tlvArr);
                        getNewPacketCond.signalAll();                        
                        while( !packetPool.containsKey(curReqId)) { 
                            getNewPacketCond.await();
                        }  
                        response = packetPool.remove(curReqId);
                    } else {
                        response = tlvArr;
                    }                                               
                    
            } finally {
                lock.unlock();
            }
            return response;  
        }
    
}
