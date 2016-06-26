package hr.fer.ztel.airqualityinpublicplaces;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import android.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.security.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Starting activity. Shows google map with POIs.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Map<Marker, POI> allMapMarkers;
    MapAreas loadedMapAreas;
    private int RATING_RADIUS = 200000; // in meters
    private int MAP_ZOOM_LEVEL = 16;
    private int LOADING_ZOOM_LEVEL = 14;
    private double ZAGREB_LAT = 45.8150;
    private double ZAGREB_LON = 15.9819;
    private GoogleApiClient client;
    public static int NEW_RATING = 10134;
    Location userLocation;

    private double poiLat;
    private double poiLon;
    private String poiName;
    private double poiRating;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        loadedMapAreas = new MapAreas();
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (userLocation==null && !gpsEnabled) {
            Toast.makeText(this, "Uključite postavke lokacije uređaja kako biste ocijenili kafiće.", Toast.LENGTH_LONG).show();
        }

        if(userLocation==null) {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (userLocation==null) {
                        Log.d("DEBUG", "LOCATION CHANGED");
                        userLocation = location;
                        moveCameraToUserLocation();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }

        moveCameraToUserLocation();

        Button OSMEditorBtn = (Button) findViewById(R.id.OSMEditorBtn);
        OSMEditorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.blau.android")));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=de.blau.android&hl=enhttps://github.com/MarcusWolschon/osmeditor4android")));
                }
            }
        });

        allMapMarkers = new HashMap<Marker, POI>();

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                Log.d("DEBUG", "MAP ZOOM: " + mMap.getCameraPosition().zoom);
                if (mMap.getCameraPosition().zoom < LOADING_ZOOM_LEVEL) { // do not load restaurants
                    Log.d("DEBUG", "Zoom too large to load restaurants...");
                    return;
                } else {
                    LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    POIClientParams changedParams = new POIClientParams(mMap, bounds, allMapMarkers, loadedMapAreas);
                    //Log.d("DEBUG", bounds.toString());
                    if (!loadedMapAreas.isLoaded(bounds)) {
                        loadedMapAreas.setLoaded(bounds);
                        new POIClient(MapsActivity.this).execute(changedParams);
                    }
                }
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {

                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    String message = "Uključite postavke lokacije kako biste mogli ocijeniti kafić.";
                    Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG).show();
                    return;
                }

                Location userLocation = mMap.getMyLocation();

                if (userLocation != null) {
                    POI POI = allMapMarkers.get(marker);
                    Location restaurantLocation = new Location("");
                    restaurantLocation.setLatitude(POI.lat);
                    restaurantLocation.setLongitude(POI.lon);

                    // Check if user is at location of Cafe bar
                    if (userLocation.distanceTo(restaurantLocation) <= RATING_RADIUS) {
                        //Log.d("DEBUG", "Click on POI: " + marker.getTitle());

                        Bundle bundle = new Bundle();
                        bundle.putString("restaurantName", POI.name);
                        bundle.putString("sensorType", POI.getSensorType(POI.id));
                        bundle.putString("lat", String.valueOf(POI.lat));
                        bundle.putString("lon", String.valueOf(POI.lon));

                        poiLat = POI.lat;
                        poiLon = POI.lon;
                        poiName = POI.name;
                        poiRating = POI.rating;
                        selectedMarker = marker;

                        Intent intent = new Intent(MapsActivity.this, RatePOIActivity.class);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, NEW_RATING);
                    } else {
                        String message = "Smijete biti udaljeni najviše " + RATING_RADIUS + "m od kafića kako biste ocijenili kvalitetu zraka.";
                        Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String message = "Nije pronađena vaša trenutna lokacija. Smijete biti udaljeni najviše " + RATING_RADIUS + "m od kafića kako biste ocijenili kvalitetu zraka.";
                    Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG).show();
                }

            }

        });
    }

    private void moveCameraToUserLocation() {
        double zoomLat = userLocation!=null?userLocation.getLatitude():ZAGREB_LAT;
        double zoomLon = userLocation!=null?userLocation.getLongitude():ZAGREB_LON;

        Log.d("DEBUG", "userLat: " + zoomLat + ", userLon: " + zoomLon);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(zoomLat, zoomLon), MAP_ZOOM_LEVEL));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(poiLat, poiLon));
            markerOptions.title(poiName);

            double rating = data.getDoubleExtra("NEW_RATING", poiRating);

            if (rating >= 1.0) {
                Log.d("DEBUG", "New rating for: " + poiName + " -> " + rating);

                paintMarker(markerOptions, rating);

                POI poi = allMapMarkers.get(selectedMarker);
                allMapMarkers.remove(selectedMarker);
                selectedMarker.remove();
                Marker newMarker = mMap.addMarker(markerOptions);
                allMapMarkers.put(newMarker, poi);
            }

        }
    }

    public static void paintMarker(MarkerOptions markerOptions, double rating) {
        if (rating >= 4.0)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        else if (rating < 4.0 && rating >= 2.5)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        else if (rating < 2.5 && rating >= 1.0)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        else {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            markerOptions.alpha(0.3f);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://hr.fer.ztel.airqualityinpublicplaces/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://hr.fer.ztel.airqualityinpublicplaces/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

}
