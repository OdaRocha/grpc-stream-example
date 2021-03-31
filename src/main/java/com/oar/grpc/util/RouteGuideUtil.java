package com.oar.grpc.util;

import com.google.protobuf.util.JsonFormat;
import com.oar.grpc.routeguide.Feature;
import com.oar.grpc.routeguide.FeatureDatabase;
import com.oar.grpc.routeguide.Point;
import com.oar.grpc.server.RouteGuideServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RouteGuideUtil {
    private static final double COORD_FACTOR = 1e7;

    /**
     * Gets the latitude for the given point.
     */
    public static double getLatitude(Point location) {
        return location.getLatitude() / COORD_FACTOR;
    }

    /**
     * Gets the longitude for the given point.
     */
    public static double getLongitude(Point location) {
        return location.getLongitude() / COORD_FACTOR;
    }

    /**
     * Indicates whether the given feature exists (i.e. has a valid name).
     */
    public static boolean exists(Feature feature) {
        return feature != null && !feature.getName().isEmpty();
    }

    /**
     * Parses the JSON input file containing the list of features.
     */
    public static List<Feature> parseFeatures() throws IOException {
        URL file = RouteGuideUtil.class.getClassLoader().getResource("routeguide/route_guide_db.json");

        try (InputStream input = file.openStream()) {
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
                JsonFormat.parser().merge(reader, database);
                return database.getFeatureList();
            }
        }
    }

    private RouteGuideUtil() throws IllegalAccessException {
        throw new IllegalAccessException();
    }
}
