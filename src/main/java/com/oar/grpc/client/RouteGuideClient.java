package com.oar.grpc.client;

import java.util.logging.Logger;

import static com.oar.grpc.server.RouteGuideServer.DEFAULT_PORT;

public class RouteGuideClient {

    public static final String SERVER_ENDPOINT = "localhost:" + DEFAULT_PORT;
    private static final Logger logger = Logger.getLogger(RouteGuideClient.class.getName());

//
//    private final GreeterBlockingStub greeterBlockingStub;
//    private final GreeterStub greeterStub;
//
//    public RouteGuideClient(Channel channel) {
//        greeterBlockingStub = GreeterGrpc.newBlockingStub(channel);
//        greeterStub = GreeterGrpc.newStub(channel);
//    }
//
//    public void sayHello(String name) {
//        HelloRequest helloReq = HelloRequest.newBuilder()
//                .setName(name)
//                .build();
//
//        HelloReply helloReply = greeterBlockingStub.sayHello(helloReq);
//        logger.info(helloReply.getMessage());
//    }
//
//    public void sayHelloAgain(String name) {
//        HelloRequest helloReq = HelloRequest.newBuilder()
//                .setName(name)
//                .build();
//
//        HelloReply helloReply = greeterBlockingStub.sayHelloAgain(helloReq);
//        logger.info(helloReply.getMessage());
//    }


}
