/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

import com.netcracker.mesh_router.ui.networks.client.rpc.RpcFuncEnum;
import com.netcracker.mesh_router.ui.networks.client.rpc.Rpc;
import com.netcracker.mesh_router.ui.networks.client.rpc.RpcBox;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NetworkRpcClient extends NetworkTcpClient{
    
    private final RpcBox rpcBox = new RpcBox();
    private final Map<Integer, Rpc> packetPool = new HashMap<>();    
                                            
    @Override
    public String createNetwork(String clientToken) throws Exception {
            
        String response = null;
        if(clientToken != null) {
            Object[] params = new Object[1];
            params[0] = clientToken;
            final Rpc resp = sendParam(new Rpc(RpcFuncEnum.CreateNetwork, getRequestID(), params));
            checkResponse(resp);
            response = (String)resp.getParams()[0];
        }
        return response;
    }
    
    @Override
    public boolean registerNetwork(String overlayID) throws Exception {
            
        if(overlayID != null) {
            Object[] params = new Object[1];
            params[0] = overlayID;
            Rpc resp = sendParam(new Rpc(RpcFuncEnum.RegisterNetwork , getRequestID(), params));
            checkResponse(resp);
            return (Boolean)resp.getParams()[0];
        }
        return false;
    }
            
    private Rpc sendParam(Rpc request) throws IOException, IllegalArgumentException, InterruptedException {
                    
        Rpc response = null;
        final int curReqId = request.getReqId();
                    
        ByteBuffer buffer = localBuffer.get();
        buffer.clear(); 
        buffer.put( rpcBox.serialize(request) );
        buffer.flip();
            
        lock.lock();             
        try {                 
            channel.write(buffer);                 
            buffer.clear();                  
            while( !packetPool.containsKey(curReqId)) {
                getNewPacketCond.await();
            }
            response = packetPool.remove(curReqId);
        } finally {
            lock.unlock();
        }
        return response;
    }
    
    private void checkResponse(final Rpc response) throws Exception {
        if(response.isException())
            throw new Exception((String)response.getParams()[0]);
    }
    
    @Override
    protected void parsePacketImpl(final ByteBuffer buffer, int position, final int length) {
        
        final List<Rpc> rpcArr = rpcBox.parse(buffer.array(), position, length);
        if(rpcArr.size() > 0) {
            rpcArr.forEach((rpc) -> {
                packetPool.put(rpc.getReqId(), rpc);
            });
        }
    }
    
}