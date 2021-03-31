package com.oar.grpc.service;

import com.google.protobuf.util.JsonFormat;
import com.oar.grpc.routeguide.Feature;
import com.oar.grpc.routeguide.FeatureDatabase;
import com.oar.grpc.routeguide.Point;
import com.oar.grpc.routeguide.Rectangle;
import com.oar.grpc.server.RouteGuideServer;
import io.grpc.stub.StreamObserver;
import com.oar.grpc.util.RouteGuideUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class RouteGuideService {

    private final Collection<Feature> features;

    public RouteGuideService() {
        this.features = getFeaturesFromFile();
    }

    /**
     * Gets the feature at the given point.
     *
     * @param location the location to check.
     * @return The feature object at the point. Note that an empty name indicates no feature.
     */
    public Feature checkFeature(Point location) {
        for (Feature feature : features) {
            if (feature.getLocation().getLatitude() == location.getLatitude()
                    && feature.getLocation().getLongitude() == location.getLongitude()) {
                return feature;
            }
        }

        // No feature was found, return an unnamed feature.
        return Feature.newBuilder().setName("").setLocation(location).build();
    }

    public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {
        int left = min(request.getLo().getLongitude(), request.getHi().getLongitude());
        int right = max(request.getLo().getLongitude(), request.getHi().getLongitude());
        int top = max(request.getLo().getLatitude(), request.getHi().getLatitude());
        int bottom = min(request.getLo().getLatitude(), request.getHi().getLatitude());

        for (Feature feature : features) {
            if (!RouteGuideUtil.exists(feature)) {
                continue;
            }

            int lat = feature.getLocation().getLatitude();
            int lon = feature.getLocation().getLongitude();
            if (lon >= left && lon <= right && lat >= bottom && lat <= top) {
                responseObserver.onNext(feature);
            }
        }
        responseObserver.onCompleted();
    }

    /**
     * Parses the JSON input file containing the list of features.
     */
    private List<Feature> getFeaturesFromFile() {
        URL file = getClass().getClassLoader().getResource("routeguide/route_guide_db.json");

        try (InputStream input = file.openStream()) {
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
                JsonFormat.parser().merge(reader, database);
                return database.getFeatureList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fail to open file route_guide_db.json .", e);
        }
    }
}
