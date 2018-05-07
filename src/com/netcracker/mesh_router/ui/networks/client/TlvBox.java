package com.netcracker.mesh_router.ui.networks.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TlvBox {
    
    private final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    
    
    public TlvBox() {        
    }
    
    public LinkedList<Tlv> parse(byte[] buffer, int offset, int length) {
        
        LinkedList<Tlv> mObjects = new LinkedList<>();
        
        int parsed = 0;
        while (parsed < length) {
            short type = ByteBuffer.wrap(buffer,offset + parsed, 2).order(DEFAULT_BYTE_ORDER).getShort();
            parsed += 2;
            short size = ByteBuffer.wrap(buffer,offset + parsed, 2).order(DEFAULT_BYTE_ORDER).getShort();
            parsed += 2;
            byte[] value = new byte[size];
            System.arraycopy(buffer, offset+parsed, value, 0, size);
            mObjects.add(new Tlv(type, value));
            parsed += size;
        }                
        return mObjects;
    }
    
    public byte[] serialize(Collection<Tlv> tlvArr) {
        int offset = 0;
        int mTotalBytes = tlvArr.stream().mapToInt((o) -> o.getFrameSize()).sum();
         
        byte[] result = new byte[mTotalBytes];                
        for (Tlv tlv : tlvArr) {
            byte[] bytes = tlv.getValue();
            byte[] type   = ByteBuffer.allocate(2).order(DEFAULT_BYTE_ORDER).putShort(tlv.getType()).array();
            byte[] length = ByteBuffer.allocate(2).order(DEFAULT_BYTE_ORDER).putShort(tlv.getLength()).array();
            System.arraycopy(type, 0, result, offset, type.length);
            offset += 2;
            System.arraycopy(length, 0, result, offset, length.length);
            offset += 2;
            System.arraycopy(bytes, 0, result, offset, bytes.length);
            offset += bytes.length;
        }
        return result;
    }
    
    public byte[] serialize(Tlv tlv) {
        
        List<Tlv> lt = new ArrayList<>();
        lt.add(tlv);
        return serialize(lt);
    }
    
    public Tlv putLong2Tlv(short type, long value) {        
        byte[] buffer = ByteBuffer.allocate(Long.BYTES).order(DEFAULT_BYTE_ORDER).putLong(value).array();
        return new Tlv(type, buffer);
    }
    
    public long getLongFromTlv(Tlv tlv) throws IllegalArgumentException {   
        if(tlv.getLength() != Long.BYTES)
            throw new IllegalArgumentException("Tlv instance doesn't contain long value!");
        return ByteBuffer.wrap(tlv.getValue(), 0, Long.BYTES).order(DEFAULT_BYTE_ORDER).getLong();
    }
    
    public Tlv putInt2Tlv(short type, int value) {        
        byte[] buffer = ByteBuffer.allocate(Integer.BYTES).order(DEFAULT_BYTE_ORDER).putInt(value).array();
        return new Tlv(type, buffer);
    }
    
    public int getIntFromTlv(Tlv tlv) throws IllegalArgumentException {   
        if(tlv.getLength() != Integer.BYTES)
            throw new IllegalArgumentException("Tlv instance doesn't contain int value!");
        return ByteBuffer.wrap(tlv.getValue(), 0, Integer.BYTES).order(DEFAULT_BYTE_ORDER).getInt();
    }
    
    public Tlv putShort2Tlv(short type, short value) {        
        byte[] buffer = ByteBuffer.allocate(Short.BYTES).order(DEFAULT_BYTE_ORDER).putShort(value).array();
        return new Tlv(type, buffer);
    }
    
    public short getShortFromTlv(Tlv tlv) throws IllegalArgumentException {   
        if(tlv.getLength() != Short.BYTES)
            throw new IllegalArgumentException("Tlv instance doesn't contain short value!");
        return ByteBuffer.wrap(tlv.getValue(), 0, Short.BYTES).order(DEFAULT_BYTE_ORDER).getShort();
    }
    
    public Tlv putByte2Tlv(short type, byte value) {        
        byte[] buffer = ByteBuffer.allocate(Byte.BYTES).order(DEFAULT_BYTE_ORDER).put(value).array();
        return new Tlv(type, buffer);
    }
    
    public byte getByteFromTlv(Tlv tlv) throws IllegalArgumentException {   
        if(tlv.getLength() != Byte.BYTES)
            throw new IllegalArgumentException("Tlv instance doesn't contain byte value!");
        return ByteBuffer.wrap(tlv.getValue(), 0, Byte.BYTES).order(DEFAULT_BYTE_ORDER).get();
    }
    
    public Tlv putDouble2Tlv(short type, double value) {        
        byte[] buffer = ByteBuffer.allocate(Double.BYTES).order(DEFAULT_BYTE_ORDER).putDouble(value).array();
        return new Tlv(type, buffer);
    }
    
    public double getDoubleFromTlv(Tlv tlv) throws IllegalArgumentException {   
        if(tlv.getLength() != Double.BYTES)
            throw new IllegalArgumentException("Tlv instance doesn't contain double value!");
        return ByteBuffer.wrap(tlv.getValue(), 0, Double.BYTES).order(DEFAULT_BYTE_ORDER).get();
    }
}
