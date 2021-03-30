package com.oar.grpc.server;

import com.oar.grpc.controller.RouteGuideController;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RouteGuideServer {

    public static final int DEFAULT_PORT = 8980;
    private static final Logger LOGGER = Logger.getLogger(RouteGuideServer.class.getName());

    private final int port;
    private final Server server;


    /** Create a RouteGuide com.oar.grpc.server listening on {@code port} */
    public RouteGuideServer() {
        this(ServerBuilder.forPort(DEFAULT_PORT), DEFAULT_PORT);
    }

    /** Create a RouteGuide com.oar.grpc.server using serverBuilder as a base and features as data. */
    public RouteGuideServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        server = serverBuilder.addService(new RouteGuideController(routeGuideService))
                .build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        LOGGER.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC com.oar.grpc.server since JVM is shutting down");
            try {
                RouteGuideServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** com.oar.grpc.server shut down");
        }));
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

}
