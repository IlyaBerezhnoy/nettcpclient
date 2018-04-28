/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netcracker.mesh_router.ui.networks.client;

/**
 *
 * @author ilia-mint
 */
public class TlvType {
    
    //predefined types
    public static final TlvType REQUEST_ID = new TlvType((short)0xA000);
    public static final TlvType NET_TOKEN = new TlvType((short)0xA001);
    public static final TlvType OVERLAY_ID = new TlvType((short)0xA002);
    public static final TlvType ERR_MSG = new TlvType((short)0xA0FF);
    
    private short _val;
    
    private TlvType(short val) {
        _val = val;
    }

    public short getVal() {
        return _val;
    }
    
//    @Override
//    public boolean equals(Object o) {
//        if (o instanceof TlvType) {
//            return ((TlvType) o).getVal() == this._val;            
//        } else if (o instanceof Long) {
//            return ((Long) o).shortValue() == this._val;
//        }
//        return false;
//    }
}
