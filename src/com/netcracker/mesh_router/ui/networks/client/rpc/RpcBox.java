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
    
    public List<Rpc> parse(byte[] buffer, int offset, int length) throws IllegalArgumentException {
        
        List<Rpc> mObjects = new ArrayList<>();
        
        int parsed = 0;
        while (parsed < length) {
            Integer funcId = parseIntParam(buffer, offset + parsed);
            parsed += Integer.BYTES;
            Integer reqId = parseIntParam(buffer, offset + parsed);
            parsed += Integer.BYTES;
            final Integer size = parseIntParam(buffer, offset + parsed);
            parsed += Integer.BYTES;
            Pair<Integer, Object[]> params = parseParams(funcId, buffer, offset+parsed, length);
            parsed += params.getKey();
            mObjects.add(new Rpc(RpcFuncEnum.valueOf(funcId), reqId, params.getValue()));
        }
      
        return mObjects;
    }
    
    public byte[] serialize(Rpc rpc) {
        
        int mTotalBytes = 0;
        int mParamBytes = 0;
        
        byte[] funcId = ByteBuffer.allocate(Integer.BYTES).order(DEFAULT_BYTE_ORDER).putInt(rpc.getFuncId().getId()).array();
        mTotalBytes += funcId.length;
        byte[] reqId = ByteBuffer.allocate(Integer.BYTES).order(DEFAULT_BYTE_ORDER).putInt(rpc.getReqId()).array();
        mTotalBytes += reqId.length;
        
        byte[][] paramsArr = null;
        Object[] rpcParams = rpc.getParams();
        if(rpc.getParams() != null) {
            paramsArr = new byte[rpcParams.length][];
            for (int i=0; i< rpcParams.length; i++) {
                paramsArr[i] = param2bytes(rpcParams[i]);
                mTotalBytes += paramsArr[i].length;
                mParamBytes += paramsArr[i].length;
            }
        }
        
        byte[] paramsSize = ByteBuffer.allocate(Integer.BYTES).order(DEFAULT_BYTE_ORDER).putInt(mParamBytes).array();
        mTotalBytes += paramsSize.length;
        
        byte[] result = new byte[mTotalBytes];
        int offset = 0;
        System.arraycopy(funcId, 0, result, offset, funcId.length);
        offset += funcId.length;
        System.arraycopy(reqId, 0, result, offset, reqId.length);
        offset += reqId.length;
        System.arraycopy(paramsSize, 0, result, offset, paramsSize.length);
        offset += paramsSize.length;
        
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
    
    protected Pair<Integer, Object[]> parseParams(Integer funcId, byte[] buffer, int pos, final int length) throws IllegalArgumentException, RuntimeException {
        
        int strBufSize = 0;
        Object[] params = null;
        Integer size = 0;
        
        switch(RpcFuncEnum.valueOf(funcId)) {
            case CreateNetwork: {
                strBufSize = parseIntParam(buffer, pos);
                pos += Integer.BYTES;
                size += Integer.BYTES;
                params = new Object[1];
                params[0] = parseStringParam(buffer, pos, strBufSize);
                pos += strBufSize;
                size += strBufSize;
                break;
            }
            case RegisterNetwork: {
                Byte result = parseByteParam(buffer, pos);
                params = new Object[1];
                params[0] = (result==1);
                pos += Byte.BYTES;
                size += Byte.BYTES;
                break;
            }
            case Exception: {
                strBufSize = parseIntParam(buffer, pos);
                pos += Integer.BYTES;
                size += Integer.BYTES;
                params = new Object[1];
                params[0] = parseStringParam(buffer, pos, strBufSize);
                pos += strBufSize;
                size += strBufSize;
                break;
            }
            default:
                throw new RuntimeException("RPC serialization failed: unknown function ID");
        }
        
        return new Pair<>(size, params);
    }
    
    protected String parseStringParam(byte[] buffer, int pos, final int length) {
        return new String(buffer, pos, length);
    }
    protected int parseIntParam(byte[] buffer, int pos){
        return ByteBuffer.wrap(buffer, pos, Integer.BYTES).order(DEFAULT_BYTE_ORDER).getInt();
    }
    protected byte parseByteParam(byte[] buffer, int pos){
        return ByteBuffer.wrap(buffer, pos, Byte.BYTES).order(DEFAULT_BYTE_ORDER).get();
    }
}
