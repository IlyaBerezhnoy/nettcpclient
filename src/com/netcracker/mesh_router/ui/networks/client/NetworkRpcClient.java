/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

import com.netcracker.mesh_router.ui.networks.client.rpc.RpcFuncEnum;
import com.netcracker.mesh_router.ui.networks.client.rpc.Rpc;
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

class NetworkRpcClient extends NetworkTcpClient{
    
    private final Condition getNewPacketCond = lock.newCondition();                    
    private Map<Long, Rpc> rpcPool = new HashMap<>();
                                        
    @Override
    public String createNetwork(String clientToken) throws IOException, IllegalArgumentException, InterruptedException {
            
            String response = null;                       
            if(clientToken != null) {
                Object[] params = new Object[1];
                params[0] = clientToken;
                Rpc resp = sendParam(new Rpc(RpcFuncEnum.CreateNetwork, (int)Thread.currentThread().getId(), params));
                if(resp != null){
                    response = (String)resp.getParams()[0];
                }
            }
            return response;                            
        }
    
        @Override
        public boolean registerNetwork(String overlayID) throws IOException, IllegalArgumentException, InterruptedException {
            
            if(overlayID != null) {   
                Object[] params = new Object[1];
                params[0] = overlayID;
                Rpc resp = sendParam(new Rpc(RpcFuncEnum.RegisterNetwork , (int)Thread.currentThread().getId(), params));
                if(resp != null) {                                        
                    return (Boolean)resp.getParams()[0];
                }
            }
            return false;
        } 
        
        private Rpc sendParam(Rpc request) throws IOException, IllegalArgumentException, InterruptedException {
                    
            Rpc response = null;
            List<Object> tlvPacket= new LinkedList<>();
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
