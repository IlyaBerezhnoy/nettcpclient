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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class NetworkTcpClient implements NetworkClientApi {
    
     protected SocketChannel channel = null;        
        protected ThreadLocal<ByteBuffer> localBuffer = 
                new ThreadLocal<ByteBuffer>()   {
                    @Override 
                    protected ByteBuffer initialValue() {
                        return ByteBuffer.allocate(4096);
                    }
                };        
        protected final Lock lock = new ReentrantLock(); 
        protected final Condition getNewPacketCond = lock.newCondition(); 
        private volatile AtomicInteger requestID = new AtomicInteger(0);
        
        public NetworkTcpClient() {            
        }
        
        public void connect(String hostName, int portNumber) throws IOException {
            lock.lock(); 
            try {
                if(channel != null && channel.isConnected())
                    channel.close(); 
                localBuffer.get().clear();
                channel = SocketChannel.open(new InetSocketAddress(hostName, portNumber));       
                channel.socket().setSoTimeout(1000);
            } finally {
                lock.unlock(); 
            }
        }
        
        public void close() throws IOException {
            lock.lock(); 
            try {
                if(channel != null && channel.isConnected())
                    channel.close();                 
            } finally {
                lock.unlock(); 
            }
        }
        
        public boolean isConnected() {
            lock.lock(); 
            try {
                return (channel != null) && channel.isConnected();                 
            } finally {
                lock.unlock(); 
            }            
        }
        
        protected int getRequestID(){
            return requestID.updateAndGet(n -> (n == Integer.MAX_VALUE) ? 0 : ++n);
        }
    
}
