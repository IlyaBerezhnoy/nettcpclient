package com.netcracker.mesh_router.ui.networks.client;

import java.io.IOException;

interface NetworkClientApi {
    
    String createNetwork(String clientToken) throws Exception;
    boolean registerNetwork(String overlayID) throws Exception;
}
