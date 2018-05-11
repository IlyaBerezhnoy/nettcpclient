/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client.rpc;

import java.util.HashMap;
import java.util.Map;

public enum FuncEnum {
    
    CreateNetwork(1),
    RegisterNetwork(2),
    Exception(~0);
    
    private static final Map<Integer, FuncEnum> funcMap;
    static{
        funcMap = new HashMap<>();
        funcMap.put(CreateNetwork.ordinal(), CreateNetwork);
        funcMap.put(RegisterNetwork.ordinal(), RegisterNetwork);
        funcMap.put(Exception.ordinal(), Exception);
    }
    
    public static FuncEnum valueOf(final int val){
        return funcMap.get(val);
    }
    
    private final int id;
    
    FuncEnum(int id) {
        this.id = id;
    } 
    
    public int getId(){
        return id;
    }
}
