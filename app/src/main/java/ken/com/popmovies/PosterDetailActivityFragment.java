package ken.com.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class PosterDetailActivityFragment extends Fragment {
    private final String LOG_TAG = GetReviewsTask.class.getSimpleName();
    private View main_view;
    public PosterDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_poster_detail, container, false);
        main_view = rootView;

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            try {
                String intentData= intent.getStringExtra(Intent.EXTRA_TEXT);
                JSONObject aMovie = new JSONObject(intentData);
                Integer id = aMovie.getInt("id");
                String title = aMovie.getString("title");
                String image = "http://image.tmdb.org/t/p/w185/" + aMovie.getString("poster_path");
                String overview = aMovie.getString("overview");
                Double vote_average = aMovie.getDouble("vote_average");
                String release_date = aMovie.getString("release_date");
                String txt = String.format("Title: %s\n Rating: %.1f\n Release Date:%s\n", title, vote_average, release_date);
                String fmt_overview = String.format("Overview:\n %s", overview);
                //Toast.makeText(getActivity(), intentData, Toast.LENGTH_SHORT).show();
                ((TextView) rootView.findViewById(R.id.fragment_poster_detail_text))
                        .setText(txt);
                ((TextView) rootView.findViewById(R.id.fragment_poster_detail_overview))
                        .setText(fmt_overview);
                GetReviewsTask reviewTask = new GetReviewsTask();
                reviewTask.execute(id.toString(),title);

                ImageButton starButton = (ImageButton) rootView.findViewById(R.id.favorite_button);
                //SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences sharedPref = sharedPref = getActivity().getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                String fav_movies = sharedPref.getString(getString(R.string.favorite_movies), "");
                List<String> movies= Arrays.asList(fav_movies.split(","));
                if (movies.indexOf(title) != -1){
                    starButton.setImageResource(android.R.drawable.btn_star_big_on);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return rootView;
    }
    public class GetReviewsTask extends AsyncTask<String, Void, TextView> {

        private final String LOG_TAG = GetReviewsTask.class.getSimpleName();
        public String movieName = "";
        private TextView getReviewDataFromJson(String reviewJsonStr)
                throws JSONException {

            JSONObject reviewJson = new JSONObject(reviewJsonStr);
            JSONArray reviewArray = reviewJson.getJSONArray("results");
            String text = "Reviews: ";
            for (int i=0;i<reviewArray.length();i++) {
                JSONObject reviewInfo = reviewArray.getJSONObject(i);
                String url = reviewInfo.getString("url");
                Integer reviewCount = i + 1;
                text += "<a href='" + url + "'> " + reviewCount + "</a> ";
            }
            TextView textView =new TextView(getContext());
            textView.setClickable(true);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(Html.fromHtml(text));
            return textView;
        }

        @Override
        protected TextView doInBackground(String... params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String reviewJsonStr = null;
            movieName = params[1];
            try {

                Integer id = Integer.parseInt(params[0]);
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/" + id + "/reviews";

                final String APPID_PARAM = "api_key";
                final String api_key = "YOURAPI_KEY_HERE";
                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(APPID_PARAM, api_key)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
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
                reviewJsonStr = buffer.toString();
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
                return getReviewDataFromJson(reviewJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }
        protected void onPostExecute(TextView result) {
            LinearLayout myLayout = (LinearLayout) main_view.findViewById(R.id.fragment_poster_info);
            myLayout.addView(result);
        }
    }

}
