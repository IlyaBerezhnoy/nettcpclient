/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client.rpc;

import javafx.util.Pair;

/**
 *
 * @author ilia-mint
 */
public class ServerRpcBox extends RpcBox {
    
    @Override
    protected Pair<Integer, Object[]> parseParams(Integer funcId, byte[] buffer, int pos, final int length) throws IllegalArgumentException, RuntimeException {
        
        int len = 0;        
        Object[] params = null;
        Integer size = 0;
        
        switch(RpcFuncEnum.valueOf(funcId)) {
            case CreateNetwork:
            case RegisterNetwork: {
                len = parseIntParam(buffer, pos);                
                pos += Integer.BYTES;
                size += Integer.BYTES;                
                params = new Object[1];
                params[0] = parseStringParam(buffer, pos, len);
                pos += len;
                size += len;
                break;
            }
            default:
                throw new RuntimeException("RPC serialization failed: unknown function ID");
        }
        
        return new Pair<>(size, params);
    }    
}
