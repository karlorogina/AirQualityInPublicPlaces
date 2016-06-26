package hr.fer.ztel.airqualityinpublicplaces;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Asinkrono obavlja slanje zahtjeva za ocjenu kvalitete zraka na web servis.
 * Created by Karlo on 08-May-16.
 */
public class PostRatingAsyncTask extends AsyncTask {
    @Override
    protected Object doInBackground(Object[] params) {
        String sensorType = (String) params[0];
        int rating = (int) params[1];
        Context context = (Context) params[2];
        String restaurantName = (String) params[3];
        String lat = (String) params[4];
        String lon = (String) params[5];

        try {
            URL url = new URL("http://161.53.19.88:3001/api/addreading");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();

            String time = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss").format(new Date());
            Log.d("DEBUG", time);

            String request = "{\r\n" +
                    "    \"type\": \"Feature\",\r\n" +
                    "    \"geometry\": {\r\n" +
                    "        \"type\": \"Point\",\r\n" +
                    "        \"coordinates\": [\r\n" +
                    "            " + lat + ",\r\n" +
                    "            " + lon + "\r\n" +
                    "        ]\r\n" +
                    "    },\r\n" +
                    "    \"properties\": {\r\n" +
                    "        \"time\": \"" + time + "\",\r\n" +
                    "        \"value\": "+rating+",\r\n" +
                    "        \"sensorType\": \""+sensorType+"\",\r\n" +
                    "        \"measurementUnit\": 9,\r\n" +
                    "        \"measurementType\": \"zadimljenost\"\r\n" +
                    "    }\r\n" +
                    "}";

            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(request);
            out.close();

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
            Log.d("DEBUG", response);

            publishProgress(restaurantName, rating, context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        String restaurantName = (String) values[0];
        int rating = (int) values[1];
        Context context = (Context) values[2];

        String message = "Ocijenili ste \"" + restaurantName + "\" ocjenom " + rating + ". Hvala!";
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}
