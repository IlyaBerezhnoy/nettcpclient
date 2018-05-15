/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client.rpc;

import java.util.HashMap;
import java.util.Map;

public enum RpcFuncEnum {
    
    CreateNetwork(1),
    RegisterNetwork(2),
    Exception(~0);
    
    private static final Map<Integer, RpcFuncEnum> funcMap;
    static{
        funcMap = new HashMap<>();
        funcMap.put(CreateNetwork.getId(), CreateNetwork);
        funcMap.put(RegisterNetwork.getId(), RegisterNetwork);
        funcMap.put(Exception.getId(), Exception);
    }
    
    public static RpcFuncEnum valueOf(final int val) throws IllegalArgumentException {
        if(!funcMap.containsKey(val))
            throw new IllegalArgumentException("Value \""+val+"\" is illegal function id");
        return funcMap.get(val);
    }
    
    private final int id;
    
    RpcFuncEnum(int id) {
        this.id = id;
    } 
    
    public int getId(){
        return id;
    }
}
