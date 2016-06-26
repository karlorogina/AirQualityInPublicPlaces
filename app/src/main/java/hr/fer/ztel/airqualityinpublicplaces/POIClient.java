package hr.fer.ztel.airqualityinpublicplaces;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Downloads POI's from OpenStreetMap and their air quality rating from web service.
 * Created by Karlo on 01-May-16.
 */
public class POIClient extends AsyncTask {

    Context mContext;
    ProgressDialog progressDialog;

    public POIClient(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage("Učitavam ugostiteljske objekte...");
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected Object doInBackground(Object[] params) {
        POIClientParams POIClientParams = (POIClientParams) params[0];
        GoogleMap mMap = POIClientParams.mMap;
        LatLngBounds bounds = POIClientParams.bounds;
        MapAreas mapAreas = POIClientParams.mapAreas;
        Map<Marker, POI> allMapMarkers = POIClientParams.allMapMarkers;
        int mRes = MapAreas.MAP_RESOLUTION;
        double north = Math.ceil(bounds.northeast.latitude*mRes)/mRes;
        double east = Math.ceil(bounds.northeast.longitude*mRes)/mRes;
        double south = Math.floor(bounds.southwest.latitude*mRes)/mRes;
        double west = Math.floor(bounds.southwest.longitude*mRes)/mRes;

        Log.d("DEBUG", "Camera bounds (NORTH, EAST, SOUTH, WEST): "+north + ", " + east + ", " + south + ", " + west);

        HashMap<String, Double> restaurantAvgRatings = new HashMap<>();
        String address = "http://161.53.19.88:3001/api/avgreadings?lonFrom=" + west + "&lonTo=" + east + "&latFrom=" + south + "&latTo=" + north + "&measurementType=zadimljenost";
        Log.d("DEBUG", address);
        try {
            URL urlws = new URL(address);
            HttpURLConnection connws = (HttpURLConnection) urlws.openConnection();
            connws.setRequestMethod("GET");

            URL urlOSM = new URL("http://overpass-api.de/api/interpreter");
            HttpURLConnection connOSM = (HttpURLConnection) urlOSM.openConnection();
            connOSM.setRequestMethod("POST");
            connOSM.setDoInput(true);
            connOSM.setDoOutput(true);
            connOSM.setRequestProperty("Content-Type", "text/xml");

            String bbox = "      <bbox-query e=\"" + east + "\" into=\"_\" n=\"" + north + "\" s=\"" + south + "\" w=\"" + west + "\"/>\n";
            String request =
                    "<osm-script output=\"json\" timeout=\"25\"> " +
                            "  <union into=\"_\"> " +
                            "    <query into=\"_\" type=\"node\"> " +
                            "      <has-kv k=\"amenity\" modv=\"\" v=\"cafe\"/> " +
                            bbox +
                            "    </query> " +
                            "    <query into=\"_\" type=\"node\"> " +
                            "      <has-kv k=\"amenity\" modv=\"\" v=\"bar\"/> " +
                            bbox +
                            "    </query> " +
                            "    <query into=\"_\" type=\"node\"> " +
                            "      <has-kv k=\"amenity\" modv=\"\" v=\"pub\"/> " +
                            bbox +
                            "    </query> " +
                            "  </union> " +
                            "  <print e=\"\" from=\"_\" geometry=\"skeleton\" limit=\"\" mode=\"body\" n=\"\" order=\"id\" s=\"\" w=\"\"/> " +
                            "  <recurse from=\"_\" into=\"_\" type=\"down\"/> " +
                            "  <print e=\"\" from=\"_\" geometry=\"skeleton\" limit=\"\" mode=\"skeleton\" n=\"\" order=\"quadtile\" s=\"\" w=\"\"/> " +
                            "</osm-script> ";

            connws.connect();
            Log.d("DEBUG", "Fetching avg ratings from web service...");
            connOSM.connect();
            OutputStreamWriter out = new OutputStreamWriter(connOSM.getOutputStream());
            out.write(request);
            Log.d("DEBUG", "Fetching POIs from OpenStreetMap...");
            out.close();

            StringBuilder sb = new StringBuilder();
            int HttpResult = connws.getResponseCode();
            Log.d("DEBUG", "Response from web service....");
            if(HttpResult==HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connws.getInputStream(), "utf-8"));
                String line = null;
                while ((line=br.readLine()) != null) {
                    sb.append(line + "\n");
                }
            }

            String response = sb.toString();

            try {
                JSONArray jsonarray = new JSONArray(response);
                for (int i = 0; i < jsonarray.length(); i++) {
                    JSONObject jsonobject = jsonarray.getJSONObject(i);
                    String key = jsonobject.getString("sensor_type");
                    Double value = jsonobject.getDouble("avg");
                    restaurantAvgRatings.put(key, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            StringBuilder sbOSM = new StringBuilder();
            int HttpResultOSM= connOSM.getResponseCode();
            if (HttpResultOSM == HttpURLConnection.HTTP_OK) {
                Log.d("DEBUG", "Response from OpenStreetMap...");
                BufferedReader br = new BufferedReader(new InputStreamReader(connOSM.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sbOSM.append(line + "\n");
                }
            }

            String responseOSM = sbOSM.toString();

            //Log.d("DEBUG", responseOSM);

            List<POI> POIs = new ArrayList<>();

            JSONObject reader = new JSONObject(responseOSM);
            JSONArray elements = reader.getJSONArray("elements");
            Log.d("DEBUG", "Elements length: " + elements.length());
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                JSONObject tags = element.getJSONObject("tags");
                if (tags.has("name")) {
                    int id = element.getInt("id");
                    String name = tags.getString("name");
                    double lat = element.getDouble("lat");
                    double lon = element.getDouble("lon");

                    String sensorType = POI.getSensorType(id);

                    double rating = restaurantAvgRatings.containsKey(sensorType)?restaurantAvgRatings.get(sensorType):0.0;

                    POI POI = new POI(id, name, lat, lon, rating);
                    if (!POIs.contains(POI))
                        POIs.add(POI);
                }
            }
            //Log.d("DEBUG", "Restaurants length: " + POIs.size());
            for (int i = 0; i < POIs.size(); i++) {
                POI r = POIs.get(i);
                Log.d("DEBUG", r.toString());
                publishProgress(r, mMap, allMapMarkers);
            }

        } catch (IOException e) {
            e.printStackTrace();
            mapAreas.setUnloaded(bounds); // Network error while downloading from OSM
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        POI POI = (POI) values[0];
        GoogleMap mMap = (GoogleMap) values[1];
        Map<Marker, POI> allMapMarkers = (Map<Marker, POI>) values[2];

        if(!allMapMarkers.containsValue(POI)) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(POI.lat, POI.lon));
            markerOptions.title(POI.name);

            MapsActivity.paintMarker(markerOptions, POI.rating);
            Marker marker = mMap.addMarker(markerOptions);
            allMapMarkers.put(marker, POI);
        }

    }

}
