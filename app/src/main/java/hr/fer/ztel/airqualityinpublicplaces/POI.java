package hr.fer.ztel.airqualityinpublicplaces;

/**
 * Point of Interest model.
 * Created by Karlo on 01-May-16.
 *
 */
public class POI {

    public int id;
    public String name;
    public double lat;
    public double lon;
    public double rating;

    public POI(int id, String name, double lat, double lon, double rating) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.rating = rating;
    }

    @Override
    public String toString() {
        return "POI{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", rating=" + rating +
                '}';
    }

    /**
     * Creates sensor type id from POI id. Each POI is represented with one sensor type.
     * @return sensorType
     */
    public static String getSensorType(int restaurantId) {
        return "Zadimljenost" + String.valueOf(restaurantId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        POI poi = (POI) o;

        if (Double.compare(poi.lat, lat) != 0) return false;
        if (Double.compare(poi.lon, lon) != 0) return false;
        return name != null ? name.equals(poi.name) : poi.name == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
