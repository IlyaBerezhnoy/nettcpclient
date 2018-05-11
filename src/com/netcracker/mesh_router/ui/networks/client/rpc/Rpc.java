/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client.rpc;

import java.util.Arrays;


public class Rpc {
    
    private final Integer funcId;
    private final Integer reqId;
    private final Object[] params;
    
    private Rpc(int funcId, int reqId, Object[] params) {
        this.funcId = funcId;
        this.reqId = reqId;
        this.params = (params != null) ? Arrays.copyOf(params, params.length) : null;
    }
    
    public Rpc(FuncEnum funcId, int reqId, Object[] params) {
        this(funcId.getId(), reqId, params);
    }
    
    public boolean isException() {
        return (funcId == ~0);
    }

    public Object[] getParams() {
        return params;
    }
    
    public Integer getFuncId() {
        return funcId;
    }

    public Integer getReqId() {
        return reqId;
    }
}
