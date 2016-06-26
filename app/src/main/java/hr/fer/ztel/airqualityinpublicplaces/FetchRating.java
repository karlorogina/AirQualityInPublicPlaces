package hr.fer.ztel.airqualityinpublicplaces;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

/**
 * Asinkrono preuzima prosjecnu ocjenu kvalitete zraka za dani restoran.
 * Created by Karlo on 08-May-16.
 */
public class FetchRating extends AsyncTask {
    @Override
    protected Object doInBackground(Object[] params) {
        String sensorType = (String) params[0];
        TextView txtRating = (TextView) params[1];
        double avgRating = 0;

        try {
            String address = "http://161.53.19.88:3001/api/avgreading?sensorType=" + sensorType;
            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //conn.setRequestProperty("Content-Length", "0");
            conn.connect();

            //Log.d("RATING", Integer.toString(conn.getResponseCode()));

            StringBuilder sb = new StringBuilder();
            int HttpResult = conn.getResponseCode();
            if(HttpResult==HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                String line = null;
                while ((line=br.readLine()) != null) {
                    sb.append(line + "\n");
                }
            }

            String response = sb.toString();

            //Log.d("RATING", response);

            try { // Ako nema nijednog ocitanja, nece se moci parsirati JSON
                JSONObject obj = new JSONObject(response);
                avgRating = obj.getDouble("avg");
            } catch (Exception e) {
                e.printStackTrace();
            }
            publishProgress(txtRating, avgRating);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return avgRating;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);

        TextView txtRating = (TextView) values[0];
        double rating = (double) values[1];
        //Log.d("RATING", "Rating: " + rating);

        DecimalFormat df = new DecimalFormat("0.#");
        txtRating.setText("Ocjena korisnika: " + df.format(rating) + "/5");
    }
}
