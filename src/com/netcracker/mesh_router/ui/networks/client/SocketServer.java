package com.netcracker.mesh_router.ui.networks.client;

import com.netcracker.mesh_router.ui.networks.client.Tlv;
import com.netcracker.mesh_router.ui.networks.client.TlvBox;
import com.netcracker.mesh_router.ui.networks.client.TlvType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ilia-mint
 */
public class SocketServer {
      
    private TlvBox tlvBox = new TlvBox();
    private static SocketServer mInst = null;
    
    private SocketServer(){} 
    private Selector selector;
    private ServerSocketChannel serverSocket;
    private ByteBuffer buffer;
    
    public static SocketServer createServer(String host, int port) throws IOException {        
        if(mInst == null){
            mInst = new SocketServer();
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
        
        try{            
            LinkedList<Tlv> tlvArr = tlvBox.parse(buffer.array(), 0, cnt);
            Long reqId = tlvBox.getLongFromTlv(tlvArr.get(0));
            Tlv tlvParam = tlvArr.remove(1);
            if(tlvParam.getType() == TlvType.NET_TOKEN.getVal()) {
                System.out.println("Get createNetwork request from "+reqId.toString()+" with token "+new String(tlvParam.getValue()));
                tlvArr.add(new Tlv(TlvType.OVERLAY_ID, ("NETWORK_OVERLAY_ID"+reqId).getBytes()));
            } else if(tlvParam.getType() == TlvType.OVERLAY_ID.getVal()) {
                System.out.println("Get registerNetwork request from "+reqId.toString()+" with overlayId "+new String(tlvParam.getValue()));
                byte[] response = {1};
                tlvArr.add(new Tlv(TlvType.OVERLAY_ID, response));
            } else {
                throw new Exception("Unknown tlv type: "+tlvArr.get(1).getType().toString());
            }
            buffer.clear();
            buffer.put(tlvBox.serialize(tlvArr));
            buffer.flip();
            client.write(buffer);
            buffer.clear();
            
//            if (new String(buffer.array()).trim().equals("GOOD BUY!")) {
//                client.close();
//                System.out.println("Not accepting client messages anymore");
//            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Error duting parsing incoming message");
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
        buffer = ByteBuffer.allocate(1024);  
        System.out.println("Server is run successfully.");
    }
    
    
}
