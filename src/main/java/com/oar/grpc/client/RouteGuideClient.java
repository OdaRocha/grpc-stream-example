package com.oar.grpc.client;

import com.oar.grpc.routeguide.*;
import com.oar.grpc.routeguide.RouteGuideGrpc.RouteGuideBlockingStub;
import com.oar.grpc.routeguide.RouteGuideGrpc.RouteGuideStub;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import com.oar.grpc.util.RouteGuideUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oar.grpc.server.RouteGuideServer.DEFAULT_PORT;

public class RouteGuideClient {

    private static final Logger logger = Logger.getLogger(RouteGuideClient.class.getName());


    private final RouteGuideBlockingStub routeGuideBlockingStub;
    private final RouteGuideStub asyncStub;
    private final Random random = new Random();


    public RouteGuideClient(Channel channel) {
        routeGuideBlockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    /**
     * Blocking unary call example.  Calls getFeature and prints the response.
     */
    public void getFeature(int lat, int lon) {
        info("*** GetFeature: lat={0} lon={1}", lat, lon);

        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

        Feature feature;
        try {
            feature = routeGuideBlockingStub.getFeature(request);
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            return;
        }
        if (RouteGuideUtil.exists(feature)) {
            info("Found feature called \"{0}\" at {1}, {2}",
                    feature.getName(),
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        } else {
            info("Found no feature at {0}, {1}",
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        }
    }

    /**
     * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints each
     * response feature as it arrives.
     */
    public void listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
        info("*** ListFeatures: lowLat={0} lowLon={1} hiLat={2} hiLon={3}", lowLat, lowLon, hiLat,
                hiLon);

        Rectangle request = Rectangle.newBuilder()
                .setLo(Point.newBuilder()
                        .setLatitude(lowLat)
                        .setLongitude(lowLon)
                        .build())
                .setHi(Point.newBuilder().
                        setLatitude(hiLat).
                        setLongitude(hiLon).
                        build())
                .build();
        try {
            Iterator<Feature> features = routeGuideBlockingStub.listFeatures(request);
            for (int i = 1; features.hasNext(); i++) {
                Feature feature = features.next();
                info("Result #" + i + ": {0}", feature);
            }
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
        }
    }

    /**
     * Async client-streaming example.
     * Sends {@code numPoints} randomly chosen points from {@code features} with a variable delay in between.
     * Prints the statistics when they are sent from the server.
     */
    public void recordRoute(List<Feature> features, int numPoints) throws InterruptedException {
        info("*** RecordRoute ***");
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<RouteSummary> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(RouteSummary summary) {
                info("Finished trip with {0} points. Passed {1} features. "
                                + "Travelled {2} meters. It took {3} seconds.", summary.getPointCount(),
                        summary.getFeatureCount(), summary.getDistance(), summary.getElapsedTime());
            }

            @Override
            public void onError(Throwable t) {
                warning("RecordRoute Failed: {0}", Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished RecordRoute");
                finishLatch.countDown();
            }
        };

        StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);
        try {
            // Send numPoints points randomly selected from the features list.
            for (int i = 0; i < numPoints; ++i) {
                int index = random.nextInt(features.size());
                Point point = features.get(index).getLocation();
                info("Visiting point {0}, {1}", RouteGuideUtil.getLatitude(point),
                        RouteGuideUtil.getLongitude(point));
                requestObserver.onNext(point);
                // Sleep for a bit before sending the next one.
                Thread.sleep(random.nextInt(1000) + 500);
                if (finishLatch.getCount() == 0) {
                    // RPC completed or errored before we finished sending.
                    // Sending further requests won't error, but they will just be thrown away.
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            warning("recordRoute can not finish within 1 minutes");
        }
    }

    /**
     * Bi-directional example, which can only be asynchronous.
     * Send some chat messages, and print any chat messages that are sent from the server.
     */
    public CountDownLatch routeChat() {
        info("*** RouteChat");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver = asyncStub.routeChat(new StreamObserver<>() {
                    @Override
                    public void onNext(RouteNote note) {
                        info("Got message \"{0}\" at {1}, {2}", note.getMessage(), note.getLocation()
                                .getLatitude(), note.getLocation().getLongitude());
                    }

                    @Override
                    public void onError(Throwable t) {
                        warning("RouteChat Failed: {0}", Status.fromThrowable(t));
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        info("Finished RouteChat");
                        finishLatch.countDown();
                    }
                });

        try {
            RouteNote[] requests = {
                    RouteNote.newBuilder()
                            .setMessage("First message")
                            .setLocation(Point.newBuilder()
                                    .setLatitude(0)
                                    .setLongitude(0)
                                    .build())
                            .build(),
                    RouteNote.newBuilder()
                            .setMessage("Second message")
                            .setLocation(Point.newBuilder()
                                    .setLatitude(0)
                                    .setLongitude(10_000_000)
                                    .build())
                            .build(),
                    RouteNote.newBuilder()
                            .setMessage("Third message")
                            .setLocation(Point.newBuilder()
                                    .setLatitude(10_000_000)
                                    .setLongitude(0)
                                    .build())
                            .build(),
                    RouteNote.newBuilder()
                            .setMessage("Fourth message")
                            .setLocation(Point.newBuilder()
                                    .setLatitude(10_000_000)
                                    .setLongitude(10_000_000)
                                    .build())
                            .build()
            };

            for (RouteNote request : requests) {
                info("Sending message \"{0}\" at {1}, {2}", request.getMessage(), request.getLocation()
                        .getLatitude(), request.getLocation().getLongitude());
                requestObserver.onNext(request);
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // return the latch while receiving happens asynchronously
        return finishLatch;
    }


    private void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    public void warning(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }

}
