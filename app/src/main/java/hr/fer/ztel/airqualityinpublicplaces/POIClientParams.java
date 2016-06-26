package hr.fer.ztel.airqualityinpublicplaces;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import java.util.Map;

/**
 * Pomocna klasa za prijenos parametara u AsyncTask POIClient.
 * Created by Karlo on 15-May-16.
 */
public class POIClientParams {
    public GoogleMap mMap;
    public LatLngBounds bounds;
    public Map<Marker, POI> allMapMarkers;
    public MapAreas mapAreas;

    public POIClientParams(GoogleMap mMap, LatLngBounds bounds, Map<Marker, POI> allMapMarkers, MapAreas mapAreas) {
        this.mMap = mMap;
        this.bounds = bounds;
        this.allMapMarkers = allMapMarkers;
        this.mapAreas = mapAreas;
    }

}
