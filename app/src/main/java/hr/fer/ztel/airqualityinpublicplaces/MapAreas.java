package hr.fer.ztel.airqualityinpublicplaces;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;

/**
 * Klasa koja dijeli mapu na geograska podrucja odredjena meridijanima i paralelama.
 * Created by Karlo on 21-May-16.
 */
public class MapAreas {
    private HashMap<LatLng, Boolean> loadedMapRegions;
    public static int MAP_RESOLUTION = 100;
    private int LATITUDE_TRANSLATION = 90;
    private int LONGITUDE_TRANSLATION = 180;

    public MapAreas() {
        this.loadedMapRegions = new HashMap<>();
    }

    /**
     * Provjerava jesu li za navedeno geografsko podrucje vec ucitani podaci o ugostiteljskim objektima.
     * @param bounds lat i long lokacije
     * @return true ako su podaci ucitani, inace false
     */
    public boolean isLoaded(LatLngBounds bounds) {
        Region region = new Region(bounds).getRegion();
        int eastStripe = region.eastStripe;
        int westStripe = region.westStripe;
        int northStripe = region.northStripe;
        int southStripe = region.southStripe;

        for(int lon=westStripe; lon<=eastStripe;lon++) {
            for(int lat=southStripe; lat<=northStripe;lat++) {
                if (!loadedMapRegions.containsKey(new LatLng(lat, lon))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Oznacava da su za odredeno geografsko podrucje ucitani objekti.
     * @param bounds lat i lon lokacije
     */
    public void setLoaded(LatLngBounds bounds) {
        Region region = new Region(bounds).getRegion();
        int eastStripe = region.eastStripe;
        int westStripe = region.westStripe;
        int northStripe = region.northStripe;
        int southStripe = region.southStripe;

        for(int lon=westStripe; lon<=eastStripe;lon++) {
            for(int lat=southStripe; lat<=northStripe;lat++) {
                loadedMapRegions.put(new LatLng(lat, lon), true);
            }
        }
    }

    public void setUnloaded(LatLngBounds bounds) {
        Region region = new Region(bounds).getRegion();
        int eastStripe = region.eastStripe;
        int westStripe = region.westStripe;
        int northStripe = region.northStripe;
        int southStripe = region.southStripe;

        for(int lon=westStripe; lon<=eastStripe;lon++) {
            for(int lat=southStripe; lat<=northStripe;lat++) {
                loadedMapRegions.put(new LatLng(lat, lon), true);
            }
        }
    }

    private class Region {
        private LatLngBounds bounds;
        public int northStripe;
        public int southStripe;
        public int eastStripe;
        public int westStripe;

        public Region(LatLngBounds bounds) {
            this.bounds = bounds;
        }

        public Region getRegion() {
            double north = bounds.northeast.latitude*MAP_RESOLUTION;
            double east = bounds.northeast.longitude*MAP_RESOLUTION;
            double south = bounds.southwest.latitude*MAP_RESOLUTION;
            double west = bounds.southwest.longitude*MAP_RESOLUTION;

            northStripe = (int) Math.floor(north) + LATITUDE_TRANSLATION * MAP_RESOLUTION;
            southStripe = (int) Math.floor(south) + LATITUDE_TRANSLATION * MAP_RESOLUTION;
            eastStripe = (int) Math.floor(east) + LONGITUDE_TRANSLATION * MAP_RESOLUTION;
            westStripe = (int) Math.floor(west) + LONGITUDE_TRANSLATION * MAP_RESOLUTION;
            return this;
        }
    }
}
