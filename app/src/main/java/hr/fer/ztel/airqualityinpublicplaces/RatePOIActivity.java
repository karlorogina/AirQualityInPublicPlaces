package hr.fer.ztel.airqualityinpublicplaces;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Aktivnost koja nudi mogucnost ocjene kvalitete zraka za restoran te prikazuje prosjecnu ocjenu kvalitete zraka.
 */
public class RatePOIActivity extends AppCompatActivity {

    private double avgRating=0.0;
    public static final String RATING_PREFS = "RatingPrefs";
    private int RATING_TIMEOUT = 15; // in minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);
        Bundle bundle = getIntent().getExtras();

        //String restaurantId = bundle.getString("restaurantId");
        final String restaurantName = bundle.getString("restaurantName");
        final String sensorType = bundle.getString("sensorType");
        final String lat = bundle.getString("lat");
        final String lon = bundle.getString("lon");
        //Log.d("OSM", "Opening restaurant details for: " + restaurantName);

        final TextView txtRating = (TextView) findViewById(R.id.rating);
        TextView txtRestaurantName = (TextView) findViewById(R.id.restaurantName);

        txtRestaurantName.setText(restaurantName);

        new FetchRating().execute(sensorType, txtRating);

        final RadioGroup ratingRadioGroup = (RadioGroup) findViewById(R.id.ratingRadioGroup);

        Button postRatingBtn = (Button) findViewById(R.id.postRatingBtn);
        assert postRatingBtn != null;
        postRatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(RATING_PREFS, 0);
                clearOldPreferences(prefs);
                boolean contains = prefs.contains(sensorType);
                if (contains) {
                    String dateString = prefs.getString(sensorType, "");
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    try {
                        Date date = formatter.parse(dateString);
                        Log.d("DEBUG", "Shared preference for this POI: " + sensorType + " -> " + date.toString());
                        Date currentDate = new Date();
                        long diff = currentDate.getTime() - date.getTime();
                        long diffMinutes = diff / (60*1000);
                        if (diffMinutes < RATING_TIMEOUT) {
                            int timeToWait = RATING_TIMEOUT - (int) diffMinutes;
                            String message = "Kafić ćete moći ponovo ocijeniti za " + timeToWait + " minuta.";
                            Toast.makeText(RatePOIActivity.this, message, Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                int selectedRadioBtnId = ratingRadioGroup.getCheckedRadioButtonId();
                int rating = 1;
                switch(selectedRadioBtnId) {
                    case R.id.radioButton1:
                        rating = 5;
                    break;
                    case R.id.radioButton2:
                        rating = 4;
                    break;
                    case R.id.radioButton3:
                        rating = 3;
                    break;
                    case R.id.radioButton4:
                        rating = 2;
                    break;
                    case R.id.radioButton5:
                        rating = 1;
                    break;
                }
                new PostRatingAsyncTask().execute(sensorType, rating, RatePOIActivity.this, restaurantName, lat, lon);
                //Log.d("RATING", "Ocjena " + rating + " za restoran " + restaurantName);
                SharedPreferences.Editor editor = prefs.edit();
                String ratingDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
                editor.putString(sensorType, ratingDate);
                editor.commit();
                Log.d("DEBUG", "Put preference: " + sensorType + "/" + ratingDate);
                try {
                    avgRating = (double) new FetchRating().execute(sensorType, txtRating).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent resultIntent = new Intent(this, MapsActivity.class);
        resultIntent.putExtra("NEW_RATING", avgRating);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void clearOldPreferences(SharedPreferences prefs) {
        Map<String, ?> map = prefs.getAll();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            Log.d("DEBUG", "Shared preferences: " + key + "/" + value);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            SharedPreferences.Editor editor = prefs.edit();
            try {
                Date date = formatter.parse(value);
                Date currentDate = new Date();
                long diff = currentDate.getTime() - date.getTime();
                long diffMinutes = diff / (60*1000);
                if (diffMinutes > RATING_TIMEOUT) {
                    editor.remove(key);
                    editor.commit();
                    Log.d("DEBUG", "Deleted: " + key + "/" + value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
