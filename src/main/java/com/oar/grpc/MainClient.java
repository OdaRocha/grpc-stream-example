package com.oar.grpc;

import com.oar.grpc.client.RouteGuideClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class MainClient {

    public static void main(String[] args) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(RouteGuideClient.SERVER_ENDPOINT).usePlaintext().build();

        try {
//            RouteGuideClient client = new RouteGuideClient(channel);
//            client.sayHello("Odair");
//            client.sayHelloAgain("Odair");

        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

}
