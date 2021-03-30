package com.oar.grpc;

import com.oar.grpc.server.RouteGuideServer;

public class MainServer {

    /**
     * Create gRPC server with default Port 8980
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        RouteGuideServer routeGuideServer = new RouteGuideServer();
        routeGuideServer.start();
        routeGuideServer.blockUntilShutdown();
    }
}
