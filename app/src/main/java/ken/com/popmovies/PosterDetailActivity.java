package ken.com.popmovies;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the Activity/View involved with clicking a Poster and viewing the Movie's details
 */
public class PosterDetailActivity extends AppCompatActivity {
    private final String LOG_TAG = PosterDetailActivity.class.getSimpleName();
    private String title = "";
    private Integer id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poster_detail_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            Intent intent = this.getIntent();
            String intentData = intent.getStringExtra(Intent.EXTRA_TEXT);
            JSONObject aMovie = new JSONObject(intentData);
            title = aMovie.getString("title");
            id = aMovie.getInt("id");
        } catch (Exception e){
            Log.e("An error occurred: ",e.getMessage());
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment, new PosterDetailActivityFragment())
                    .commit();
        }
    }

    /**
     * Launch a trailer
     */
    public void LaunchTrailer(View v){
        Intent intent = this.getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            FetchTrailerTask trailerTask = new FetchTrailerTask();
            trailerTask.execute(id.toString(),title);
        }
    }

    /**
     * Toggle the star button and Movie favorites
     */
    public void ToggleStar(View v){
        ImageButton starButton = (ImageButton) findViewById(R.id.favorite_button);
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String fav_movies = sharedPref.getString(getString(R.string.favorite_movies), "");
        List<String> movies= new ArrayList<String>(
                            Arrays.asList(
                                    fav_movies.split(",")
                            ));
        int index = movies.indexOf(title);

        // Get the initial tag value of the button (will tell us if star is "on" or not
        int starred = Integer.parseInt((String) starButton.getTag());
        int starValue = Integer.valueOf(android.R.drawable.btn_star_big_on);
        // If star is not "on", turn on and mark Movie as a favorite
        if (starred != starValue){
            if (index == -1){
                movies.add(title);
            }
            starButton.setImageResource(android.R.drawable.btn_star_big_on);
            starButton.setTag(Integer.toString(android.R.drawable.btn_star_big_on));
        }
        // Otherwise, unfavorite
        else {
            if (index != -1){
                movies.remove(index);
            }
            starButton.setImageResource(android.R.drawable.btn_star_big_off);
            starButton.setTag(Integer.toString(android.R.drawable.btn_star_big_off));
        }

        String updated_movies = TextUtils.join(",", movies);
        editor.putString(getString(R.string.favorite_movies), updated_movies);
        editor.commit();
    }

    /**
     * AsyncTask for launching a trailer video
     */
    public class FetchTrailerTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchTrailerTask.class.getSimpleName();
        public String movieName = "";
        private String getTrailerDataFromJson(String trailerJsonStr)
                throws JSONException {
            JSONObject trailerJson = new JSONObject(trailerJsonStr);
            JSONArray trailerArray = trailerJson.getJSONArray("results");
            String key = "";

            if (trailerArray.length() > 0) {

                JSONObject trailerInfo = trailerArray.getJSONObject(0);
                String site = trailerInfo.getString("site");
                if (site.equals("YouTube")) {
                    key = trailerInfo.getString("key");
                }
            }
            return key;
        }

        @Override
        protected String doInBackground(String... params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String trailerJsonStr = null;
            movieName = params[1];
            try {

                Integer id = Integer.parseInt(params[0]);
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/" + id + "/videos";

                final String APPID_PARAM = "api_key";
                final String api_key = "YOURAPI_KEY_HERE";
                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(APPID_PARAM, api_key)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                trailerJsonStr = buffer.toString();

                //Log.v(LOG_TAG, "Trailer string: " + trailerJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getTrailerDataFromJson(trailerJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the movie data.
            return null;
        }
        protected void onPostExecute(String result) {
            if (result.length() > 0) {
               // Log.v(LOG_TAG,"http://www.youtube.com/watch?v=" + result);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + result)));
            }
            else{
                AlertDialog.Builder noTrailerAlert = new AlertDialog.Builder(PosterDetailActivity.this);
                noTrailerAlert.setMessage("No Trailer found for " + movieName);
                noTrailerAlert.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                noTrailerAlert.show();
            }
        }
    }
}
