package com.example.navigatorz;

import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.api.tilequery.MapboxTilequery;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.provider.Settings.System.getString;

public class ExtractGeoJson {
//    private MapboxMap mapboxMap;
//
//    public ExtractGeoJson(MapboxMap mapboxMap) {
//        this.mapboxMap = mapboxMap;
//    }
//
//    /**
//     * Use the Java SDK's MapboxTilequery class to build a API request and use the API response
//     *
//     * @param point the center point that the the tilequery will originate from.
//     */
//    private void makeTilequeryApiCall(@NonNull LatLng point) {
//        MapboxTilequery tilequery = MapboxTilequery.builder()
//                .accessToken(String.valueOf(R.string.access_token))
//                .mapIds("mapbox.mapbox-streets-v8")
//                .query(Point.fromLngLat(point.getLongitude(), point.getLatitude()))
//                .radius(143)
//                .limit(8)
//                .geometry("point")
//                .dedupe(true)
//                .layers("poi_label")
//                .build();
//
//        tilequery.enqueueCall(new Callback<FeatureCollection>() {
//            @Override
//            public void onResponse(Call<FeatureCollection> call, Response<FeatureCollection> response) {
//                if (response.body() != null) {
//                    FeatureCollection responseFeatureCollection = response.body();
//                    if (responseFeatureCollection.features() != null) {
//                        List<Feature> featureList = responseFeatureCollection.features();
//                        if (featureList.isEmpty()) {
//                            boolean empty = true;
//                        } else {
//                            tilequerylocs = new ArrayList<>();
//                            poi = new HashMap<>();
//                            Log.d("FEATURELIST-POI", featureList.toString());
//
//                            extractDataPOI(featureList, point);
//
//                        }
//                    }
//                    mapboxMap.getStyle(new Style.OnStyleLoaded() {
//                        @Override
//                        public void onStyleLoaded(@NonNull Style style) {
//                            GeoJsonSource resultSource = style.getSourceAs(RESULT_GEOJSON_SOURCE_ID);
//                            //GeoJsonSource locationSource = style.getSourceAs(LOCATION_CENTER_GEOJSON_SOURCE_ID);
//
//                            if (resultSource != null && responseFeatureCollection.features() != null) {
//                                List<Feature> featureList = responseFeatureCollection.features();
//                                //locationSource.setGeoJson(Point.fromLngLat(point.getLongitude(), point.getLatitude()));
//                                if (featureList.isEmpty()) {
//                                    boolean empty = true;
//                                } else {
//                                    resultSource.setGeoJson(FeatureCollection.fromFeatures(featureList));
//
//                                }
//                            }
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onFailure(Call<FeatureCollection> call, Throwable throwable) {
//                Log.d("Request failed: %s", throwable.getMessage());
//                Toast.makeText(MapsActivity.this, R.string.api_error, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Use the Java SDK's MapboxTilequery class to build a API request and use the API response
//     *
//     * @param point the center point that the the tilequery will originate from.
//     */
//    private void makeTilequeryApiRoadCall(@NonNull LatLng point) {
//        MapboxTilequery tilequery = MapboxTilequery.builder()
//                .accessToken(getString(R.string.access_token))
//                .mapIds("mapbox.mapbox-streets-v8")
//                .query(Point.fromLngLat(point.getLongitude(), point.getLatitude()))
//                .radius(60)
//                .limit(1)
//                .geometry("linestring")
//                .dedupe(true)
//                .layers("road")
//                .build();
//
//        tilequery.enqueueCall(new Callback<FeatureCollection>() {
//            @Override
//            public void onResponse(Call<FeatureCollection> call, Response<FeatureCollection> response) {
//                if (response.body() != null) {
//                    FeatureCollection responseFeatureCollection = response.body();
//                    if (responseFeatureCollection.features() != null) {
//                        List<Feature> featureList = responseFeatureCollection.features();
//                        if (featureList.isEmpty()) {
//                            Toast.makeText(MapsActivity.this,
//                                    getString(R.string.no_tilequery_response_features_toast), Toast.LENGTH_SHORT).show();
//                        } else {
//                            roadName = "";
//                            Log.d("FEATURELIST-ROAD", featureList.toString());
//                            extractDataRoad(featureList);
//
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<FeatureCollection> call, Throwable throwable) {
//                Log.d("Request failed: %s", throwable.getMessage());
//                Toast.makeText(MapsActivity.this, R.string.api_error, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    public void extractDataRoad(List<Feature> featureList) {
//        for (int i =0; i<featureList.size(); i++) {
//            Feature feature = featureList.get(i);
//
//            JsonObject tile = feature.getProperty("tilequery").getAsJsonObject();
//
//            JsonObject props = feature.properties();
//            if (tile.get("layer").getAsString().equals("road") && tile.get("geometry").getAsString().equals("linestring") && props.get("name")!=null) {
//                roadName = props.get("name").getAsString();
//                break;
//            }
//        }
//    }
//
//    public void extractDataPOI(List<Feature> featureList, LatLng point) {
//        for (int i = 0; i < featureList.size(); i++) {
//            Feature feature = featureList.get(i);
//            Log.d("ExtractDataPOI:", feature.toString());
//
//            JsonObject tile = feature.getProperty("tilequery").getAsJsonObject();
//
//            JsonObject props = feature.properties();
//            try {
//                props.get("name").getAsString();
//            } catch (Exception e) {
//                continue;
//            }
//
//            Point p = (Point) feature.geometry();
//
//            if (props != null && tile != null && p != null) {
//
//                String location_name = props.get("name").getAsString();
//                String location_type = "";
//                if (props.get("category_en") != null) {
//                    location_type = props.get("category_en").getAsString();
//                }
//
//                double distance = (double) Math.round(tile.get("distance").getAsDouble());
//
//                double longitude = p.longitude();
//                double latitude = p.latitude();
//
//                Location loc = new Location("");
//                loc.setLongitude(longitude);
//                loc.setLatitude(latitude);
//                tilequerylocs.add(loc);
//
//                ArrayList<String> details = new ArrayList<>();
//                details.add(location_type);
//                details.add(Double.toString(distance));
//                poi.put(location_name, details);
//            }
//        }
//    }

}
