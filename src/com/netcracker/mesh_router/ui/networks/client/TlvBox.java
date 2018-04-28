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
    
    public LinkedList<Tlv> parse(byte[] buffer, int offset, int length) throws IllegalArgumentException {
        
        LinkedList<Tlv> mObjects = new LinkedList<>();
        
        int parsed = 0;
        while (parsed < length) {
            short type = ByteBuffer.wrap(buffer,offset + parsed, 2).order(DEFAULT_BYTE_ORDER).getShort();
            parsed += 2;
            short size = ByteBuffer.wrap(buffer,offset + parsed, 2).order(DEFAULT_BYTE_ORDER).getShort();
            parsed += 2;
            byte[] value = new byte[size];
            System.arraycopy(buffer, offset+parsed, value, 0, size);
            mObjects.push(new Tlv(type, value));
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
            byte[] type   = ByteBuffer.allocate(2).order(DEFAULT_BYTE_ORDER).putInt(tlv.getType()).array();
            byte[] length = ByteBuffer.allocate(2).order(DEFAULT_BYTE_ORDER).putInt(tlv.getLength()).array();
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
            throw new IllegalArgumentException("Tlv instance doesn't conatin long value!");
        return ByteBuffer.wrap(tlv.getValue(), 0, Long.BYTES).order(DEFAULT_BYTE_ORDER).getLong();
    }
}
