/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client.rpc;

import static com.netcracker.mesh_router.ui.networks.client.rpc.RpcFuncEnum.CreateNetwork;
import static com.netcracker.mesh_router.ui.networks.client.rpc.RpcFuncEnum.RegisterNetwork;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class NetworkRpcServer {
    private RpcBox rpcBox = new ServerRpcBox();
    private static NetworkRpcServer mInst = null;
    
    private NetworkRpcServer(){} 
    private Selector selector;
    private ServerSocketChannel serverSocket;
    private ByteBuffer buffer;
    
    public static NetworkRpcServer createServer(String host, int port) throws IOException {        
        if(mInst == null){
            mInst = new NetworkRpcServer();
            mInst.init(host, port);
        }
        return mInst;
    }
    
    public void Run() throws Exception {
        while (true) {  
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
 
                SelectionKey key = iter.next();
 
                if (key.isAcceptable()) {
                    register(selector, serverSocket);                    
                }
 
                if (key.isReadable()) {                     
                    answer(buffer, key);
                }
                iter.remove();
            }
        }
    }
    
    private void answer(ByteBuffer buffer, SelectionKey key) 
            throws IOException, Exception {
        
        SocketChannel client = (SocketChannel) key.channel();        
        int cnt = client.read(buffer);
        
        //if(cnt <= 0)
        //    return;
        
        try{            
            List<Rpc> requestArr = rpcBox.parse(buffer.array(), 0, cnt);
           
            if(requestArr.size() > 0)
            {                
                Rpc rpcResponse = null; 
                Integer reqId = 0;
                RpcFuncEnum funcId;
                for(Rpc rpcReq : requestArr){
                    reqId = rpcReq.getReqId();
                    funcId = rpcReq.getFuncId();
                    switch(funcId) {
                        case CreateNetwork: {
                            System.out.println("Get createNetwork request #"+reqId+" with token "+rpcReq.getParams()[0]);
                            if(Math.random() > 0.5f)
                                rpcResponse = new Rpc(funcId, reqId, new Object[]{("NETWORK_OVERLAY_ID"+reqId)});                            
                            else
                                rpcResponse = new Rpc(RpcFuncEnum.Exception, reqId, new Object[]{("SERVER RANDOM EXCEPTION FOR "+reqId)});                            
                            break;
                        }
                        case RegisterNetwork: {
                            System.out.println("Get registerNetwork request #"+reqId+" with overlayId "+rpcReq.getParams()[0]);
                            if(Math.random() > 0.5f)
                                rpcResponse = new Rpc(funcId, reqId, new Object[]{(byte)(reqId%2)});
                            else
                                rpcResponse = new Rpc(RpcFuncEnum.Exception, reqId, new Object[]{("SERVER RANDOM EXCEPTION FOR "+reqId)});                                                        
                            break;
                        }
                        default:
                            rpcResponse = new Rpc(RpcFuncEnum.Exception, reqId, new Object[]{("SERVER EXCEPTION: unknown function ID")});
                    }                    
                    buffer.clear();
                    buffer.put(rpcBox.serialize(rpcResponse));
                    buffer.flip();
                    client.write(buffer);
                    buffer.clear();
                }
            }
            
//            if (new String(buffer.array()).trim().equals("GOOD BUY!")) {
//                client.close();
//                System.out.println("Not accepting client messages anymore");
//            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Error during parsing incoming message");
            System.out.println(ex.getMessage());
        }
    }

    private void register(Selector selector, ServerSocketChannel serverSocket)
        throws IOException {

        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);        
    }
    
    private void init(String host, int port) throws IOException {        
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        buffer = ByteBuffer.allocate(4096);  
        System.out.println("Server is run successfully.");
    }
}
