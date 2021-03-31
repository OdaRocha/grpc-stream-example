package com.oar.grpc;

import com.oar.grpc.client.RouteGuideClient;
import com.oar.grpc.routeguide.Feature;
import com.oar.grpc.util.RouteGuideUtil;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainClient {

    public static void main(String[] args) throws Exception {
        String target = "localhost:8980";
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [target]");
                System.err.println("");
                System.err.println("  target  The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            target = args[0];
        }

        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        try {
            RouteGuideClient client = new RouteGuideClient(channel);

            // Looking for a valid feature
            // Unary Client and Server
            client.getFeature(409146138, -746188906);

            // Feature missing.
            // Unary Client and Server
            client.getFeature(0, 0);

            // Looking for features between 40, -75 and 42, -73.
            // Unary Client and Steam Server
            client.listFeatures(400000000, -750000000, 420000000, -730000000);

            // Record a few randomly selected points from the features file.
            // Stream Client and Unary Server
            client.recordRoute(features, 10);

            // Send and receive some notes.
            // Stream Client and Server
            CountDownLatch finishLatch = client.routeChat();

            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                client.warning("routeChat can not finish within 1 minutes");
            }
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

}
