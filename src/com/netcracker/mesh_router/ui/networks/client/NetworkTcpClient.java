/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

import java.io.IOException;
import static java.lang.Thread.MAX_PRIORITY;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class NetworkTcpClient implements NetworkClientApi {
    
        private volatile PacketHandlerThread thread = new PacketHandlerThread();
        private volatile Selector selector = null;
        protected volatile SocketChannel channel = null;
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
            thread.setDaemon(true);
        }
        
        public void connect(String hostName, int portNumber) throws IOException {
            lock.lock(); 
            try {  
                //if(thread.isAlive())
                 //   thread.interrupt();
                //TODO wait  until thread has stopped
                if(selector == null)
                    selector = Selector.open();
                
                SocketChannel newChannel = SocketChannel.open(new InetSocketAddress(hostName, portNumber));
                newChannel.configureBlocking(false);
                newChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                
                if(channel != null)
                    channel.close();
                
                channel = newChannel;                
                 
                //thread.interrupted();
                thread.start(); 
               
            } finally {
                lock.unlock(); 
            }
        }
        
        public void close() throws IOException {
            lock.lock(); 
            try {
                if(channel != null)
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
        
        abstract protected void parsePacketImpl(final ByteBuffer buffer, int position, final int length);
        
        private class PacketHandlerThread extends Thread {
            
            @Override
            public void run() {
                
                while (true) {
                    
                   // while(Thread.currentThread().isInterrupted()) {
                        //System.out.println("wait");
                    //    Thread.yield();
                   // }
                    
                    try {
                        selector.select();
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iter = selectedKeys.iterator();
                        while (iter.hasNext()) {

                            SelectionKey key = iter.next();
                            if (key.isReadable()) {                                                                       
                                lock.lock(); 
                                try {
                                    //TODO check the buffer size and bytes received count
                                    ByteBuffer buffer = localBuffer.get();
                                    buffer.clear();
                                    int readBytes = channel.read(buffer);
                                    if(readBytes > 0) {                                              
                                        parsePacketImpl(buffer, 0, readBytes); 
                                        getNewPacketCond.signalAll();
                                    }                        
                                } finally {
                                    lock.unlock();
                                }
                            }
                            iter.remove();
                        }
                    } catch(IOException ioEx) {
                        //TODO handle Exception
                    }
                    Thread.yield();
                }                               
            }
        }
}
