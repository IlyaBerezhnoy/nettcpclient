/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client.rpc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;

/**
 *
 * @author ilia-mint
 */
public class RpcBox {
    
    private final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    
    public RpcBox(){}
    
    public List<Rpc> parse(byte[] buffer, int offset, int length) {
        
        List<Rpc> mObjects = new ArrayList<>();
        
        int parsed = 0;
        while (parsed < length) {
            Integer funcId = ByteBuffer.wrap(buffer, offset + parsed, Integer.BYTES).order(DEFAULT_BYTE_ORDER).getInt();
            parsed += Integer.BYTES;
            Integer reqId = ByteBuffer.wrap(buffer,offset + parsed, Integer.BYTES).order(DEFAULT_BYTE_ORDER).getInt();
            parsed += Integer.BYTES;
            Pair<Integer, Object[]> params = parseParams(funcId, buffer, offset+parsed, length);
            parsed += params.getKey();
            mObjects.add(new Rpc(funcId, reqId, params.getValue()));            
        }                
        return mObjects;
    }
    
    public byte[] serialize(Rpc rpc) {
        
        int mTotalBytes = 0;
        
        byte[] funcId = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(rpc.getFuncId()).array();
        mTotalBytes += funcId.length;
        byte[] reqId = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(rpc.getReqId()).array();
        mTotalBytes += reqId.length;    
        
        byte[][] paramsArr = null;
        Object[] rpcParams = rpc.getParams();
        if(rpc.getParams() != null) {            
            paramsArr = new byte[rpcParams.length][];
            for (int i=0; i< rpcParams.length; i++) {
                paramsArr[i] = param2bytes(rpcParams[i]);                
                mTotalBytes += paramsArr[i].length;                
            }
        }
        
        byte[] result = new byte[mTotalBytes];  
        int offset = 0;
        System.arraycopy(funcId, 0, result, offset, funcId.length);
        offset += funcId.length;
        System.arraycopy(reqId, 0, result, offset, reqId.length);
        offset += reqId.length;
        
        if(paramsArr != null) {
            for (int i=0; i< paramsArr.length; i++) {
                System.arraycopy(paramsArr[i], 0, result, offset, paramsArr[i].length);
                offset += paramsArr[i].length;
            }
        }
        return result;
    }
    
    private byte[] param2bytes(Object param) throws RuntimeException {
        
        if(param instanceof Boolean) {
            return ByteBuffer.allocate(Byte.BYTES).order(DEFAULT_BYTE_ORDER).put((byte)((Boolean)param ? 1 : 0)).array();
        } else if(param instanceof Byte) {
            return ByteBuffer.allocate(Byte.BYTES).order(DEFAULT_BYTE_ORDER).put((Byte)param).array();
        } else if(param instanceof Short) {
            return ByteBuffer.allocate(Short.BYTES).order(DEFAULT_BYTE_ORDER).putShort((Short)param).array();
        } else if(param instanceof Integer) {
            return ByteBuffer.allocate(Integer.BYTES).order(DEFAULT_BYTE_ORDER).putInt((Integer)param).array();
        } else if(param instanceof Long) {
            return ByteBuffer.allocate(Long.BYTES).order(DEFAULT_BYTE_ORDER).putLong((Long)param).array();
        } else if(param instanceof String) {            
            String str = (String)param;            
            return ByteBuffer.allocate(str.getBytes().length+Integer.BYTES).order(DEFAULT_BYTE_ORDER)
                    .putInt(str.getBytes().length)
                    .put(str.getBytes()).array();
        } else {
            throw new RuntimeException("RPC serialization failed: unknown type of RPC parameter!");
        }
    }
    
    private Pair<Integer, Object[]> parseParams(Integer funcId, byte[] buffer, int pos, final int length) throws IllegalArgumentException, RuntimeException {
        
        int len = 0;        
        Object[] params = null;
        Integer size = 0;
        
        switch(FuncEnum.valueOf(funcId)){
            case CreateNetwork: {
                len = parseIntParam(buffer, pos, length);
                pos += Integer.BYTES;
                size += Integer.BYTES;                
                params = new Object[1];
                params[0] = parseStringParam(buffer, pos, length);
                pos += len;
                size += len;
                break;
            }
            case RegisterNetwork: {
                Byte result = parseByteParam(buffer, pos, length);
                params = new Object[1];
                params[0] = (result==1);
                pos += Byte.BYTES;
                size += Byte.BYTES;
                break;
            }
            case Exception: {
                len = parseIntParam(buffer, pos, length);
                pos += Integer.BYTES;
                size += Integer.BYTES;                
                params = new Object[1];
                params[0] = parseStringParam(buffer, pos, length);
                pos += len;
                size += len;
                break;
            }
            default:
                throw new RuntimeException("RPC serialization failed: unknown function ID");
        }
        
        return new Pair<>(size, params);
    }
    
    private String parseStringParam(byte[] buffer, int pos, final int length) {
        final int size = ByteBuffer.wrap(buffer, pos, Integer.BYTES).order(DEFAULT_BYTE_ORDER).getInt();
        pos += Integer.BYTES;
        return new String(buffer, pos, size);
    }
    private int parseIntParam(byte[] buffer, int pos, final int length){
        return ByteBuffer.wrap(buffer, pos, Byte.BYTES).order(DEFAULT_BYTE_ORDER).getInt();
    }
    private byte parseByteParam(byte[] buffer, int pos, final int length){
        return ByteBuffer.wrap(buffer, pos, Byte.BYTES).order(DEFAULT_BYTE_ORDER).get();
    }
}
