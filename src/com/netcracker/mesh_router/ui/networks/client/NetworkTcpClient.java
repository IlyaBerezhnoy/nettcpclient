/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author ilia-mint
 */
abstract class NetworkTcpClient implements NetworkClientApi {
    
     protected SocketChannel channel = null;        
        protected ThreadLocal<ByteBuffer> localBuffer = 
                new ThreadLocal<ByteBuffer>()   {
                    @Override 
                    protected ByteBuffer initialValue() {
                        return ByteBuffer.allocate(1024);
                    }
                };        
        protected final Lock lock = new ReentrantLock(); 
        
        public NetworkTcpClient() {            
        }
        
        public void connect(String hostName, int portNumber) throws IOException {
            lock.lock(); 
            try {
                if(channel != null && channel.isConnected())
                    channel.close(); 
                localBuffer.get().clear();
                channel = SocketChannel.open(new InetSocketAddress(hostName, portNumber));                
            } finally {
                lock.unlock(); 
            }
        }
    
}
