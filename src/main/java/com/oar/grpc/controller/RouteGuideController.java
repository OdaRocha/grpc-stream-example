package com.oar.grpc.controller;

import com.oar.grpc.routeguide.*;
import com.oar.grpc.routeguide.RouteGuideGrpc.RouteGuideImplBase;
import com.oar.grpc.service.RouteGuideService;
import io.grpc.stub.StreamObserver;
import util.RouteGuideUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class RouteGuideController extends RouteGuideImplBase {

    private static final Logger logger = Logger.getLogger(RouteGuideController.class.getName());

    private final RouteGuideService routeGuideService;
    private final ConcurrentMap<Point, List<RouteNote>> routeNotes = new ConcurrentHashMap<Point, List<RouteNote>>();

    public RouteGuideController() {
        this.routeGuideService = new RouteGuideService();
    }


    @Override
    public void getFeature(Point request, StreamObserver<Feature> responseObserver) {
        responseObserver.onNext(routeGuideService.checkFeature(request));
        responseObserver.onCompleted();
    }

    @Override
    public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {
        routeGuideService.listFeatures(request, responseObserver);
    }

    @Override
    public StreamObserver<Point> recordRoute(StreamObserver<RouteSummary> responseObserver) {
        return new StreamObserver<>() {
            int pointCount;
            int featureCount;
            int distance;
            Point previous;
            final long startTime = System.nanoTime();

            @Override
            public void onNext(Point point) {
                pointCount++;
                if (RouteGuideUtil.exists(routeGuideService.checkFeature(point))) {
                    featureCount++;
                }
                // For each point after the first, add the incremental distance from the previous point to
                // the total distance value.
                if (previous != null) {
                    distance += calcDistance(previous, point);
                }
                previous = point;
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "recordRoute cancelled");
            }

            @Override
            public void onCompleted() {
                long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                responseObserver.onNext(RouteSummary.newBuilder()
                        .setPointCount(pointCount)
                        .setFeatureCount(featureCount)
                        .setDistance(distance)
                        .setElapsedTime((int) seconds)
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<RouteNote> routeChat(StreamObserver<RouteNote> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(RouteNote note) {
                List<RouteNote> notes = getOrCreateNotes(note.getLocation());

                // Respond with all previous notes at this location.
                for (RouteNote prevNote : notes.toArray(new RouteNote[0])) {
                    responseObserver.onNext(prevNote);
                }

                // Now add the new note to the list
                notes.add(note);
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "routeChat cancelled");
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Get the notes list for the given location. If missing, create it.
     */
    private List<RouteNote> getOrCreateNotes(Point location) {
        List<RouteNote> notes = Collections.synchronizedList(new ArrayList<>());
        List<RouteNote> prevNotes = routeNotes.putIfAbsent(location, notes);
        return prevNotes != null ? prevNotes : notes;
    }

    /**
     * Calculate the distance between two points using the "haversine" formula.
     * The formula is based on http://mathforum.org/library/drmath/view/51879.html.
     *
     * @param start The starting point
     * @param end   The end point
     * @return The distance between the points in meters
     */
    private static int calcDistance(Point start, Point end) {
        int r = 6371000; // earth radius in meters
        double lat1 = toRadians(RouteGuideUtil.getLatitude(start));
        double lat2 = toRadians(RouteGuideUtil.getLatitude(end));
        double lon1 = toRadians(RouteGuideUtil.getLongitude(start));
        double lon2 = toRadians(RouteGuideUtil.getLongitude(end));
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = sin(deltaLat / 2) * sin(deltaLat / 2)
                + cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        return (int) (r * c);
    }

}
