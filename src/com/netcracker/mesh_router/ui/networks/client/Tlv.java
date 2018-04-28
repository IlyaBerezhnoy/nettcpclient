package com.netcracker.mesh_router.ui.networks.client;

import java.util.List;
import java.util.zip.DataFormatException;

public class Tlv {
       
    private short type;
    private byte[] value;

    public Tlv(short type, byte[] value) throws IllegalArgumentException {
        
        if(value == null)
                throw new IllegalArgumentException("TLV value can't ne null");
        
        this.type = type;
        this.value = value;
    }
    
    public Tlv(TlvType preType, byte[] value) throws IllegalArgumentException {
        this(preType.getVal(), value);        
    }
    
    
    public Short getType() {
        return type;
    }
    
    public short getLength() {
        return (short)value.length;
    }

    public byte[] getValue() {
        return value;
    }
    
    public int getFrameSize(){
        return 2*Short.BYTES+value.length;
    }
}
