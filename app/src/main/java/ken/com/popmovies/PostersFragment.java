package ken.com.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *The Fragment responsible for generating the Poster images grid
 */
public class PostersFragment extends Fragment {

    private static ImageAdapter movieAdapter;
    private static final int num_posters = 12;

    private GridView g_view;
    public PostersFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.posters_fragment, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            FetchMoviesTask movieTask = new FetchMoviesTask();
            movieTask.execute("lol");
            g_view.setAdapter(movieAdapter);
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this.getActivity(), SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.poster_main, container, false);
        GridView gridview = (GridView) rootView.findViewById(R.id.poster_gridview);
        g_view = gridview;
        movieAdapter = new ImageAdapter(getContext());
        gridview.setAdapter(movieAdapter);
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Movie m = movieAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), PosterDetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, m.json);
                startActivity(intent);
            }
        });
        return rootView;
    }
    @Override
    public void onStart() {
        super.onStart();
        FetchMoviesTask movieTask = new FetchMoviesTask();
        movieTask.execute("lol");
    }
    public class FetchMoviesTask extends AsyncTask<String, Void, List<Movie>> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        /**
         * Parse the JSON for all our movie data needed to build a Movie object
         */
        //FIRST ELEMENT {
        // "poster_path":"\/lIv1QinFqz4dlp5U4lQ6HaiskOZ.jpg",
        // "overview":"Under the direction of a ruthless instructor, a talented young drummer begins to pursue perfection at any cost, even his humanity.",
        // "release_date":"2014-10-10",
        // "original_title":"Whiplash",
        // "title":"Whiplash",
        // "vote_average":8.34}
        private List<Movie> getMovieDataFromJson(String movieJsonStr)
                throws JSONException {
            List<Movie> movies = new ArrayList<Movie>();
            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = movieJson.getJSONArray("results");
            for (int i=0;i<movieArray.length() && i < num_posters;i++){
                JSONObject aMovie = movieArray.getJSONObject(i);
                String title = aMovie.getString("title");
                String image = "http://image.tmdb.org/t/p/w300/" + aMovie.getString("poster_path");
                String overview = aMovie.getString("overview");
                Double vote_average = aMovie.getDouble("vote_average");
                Double popularity = aMovie.getDouble("popularity");
                String release_date = aMovie.getString("release_date");
                Integer id = aMovie.getInt("id");
                Movie movie = new Movie(title,image,overview,vote_average,release_date,aMovie.toString(),popularity,id);
                movies.add(movie);
            }
            return movies;
        }

        @Override
        protected List<Movie> doInBackground(String... params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieJsonStr = null;

            try {
                // Construct the URL for the themoviedb query
                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/top_rated";

                final String APPID_PARAM = "api_key";

                final String api_key = "YOURAPI_KEY_HERE";
                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(APPID_PARAM, api_key)
                        .build();

                URL url = new URL(builtUri.toString());

               // Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to the movie database, and open the connection
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
                movieJsonStr = buffer.toString();

                //Log.v(LOG_TAG, "Movie string: " + movieJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
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
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }
        protected void onPostExecute(List<Movie> result) {

            if (result != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                String sortBy = prefs.getString(getString(R.string.pref_sort_key), getString(R.string.pref_sort_type_popular));
                // Sort the movies according to the sort type
                boolean showFavs = prefs.getBoolean(getString(R.string.pref_fav_key), false);
                if (showFavs){
                    result = ShowOnlyFavorites(result);
                }
                Comparator<Movie> comp;
                if (sortBy.equals("Popularity")) {
                    comp = new Comparator<Movie>() {
                        public int compare(Movie m1, Movie m2) {
                            return Double.compare(m1.popularity, m2.popularity);
                        }
                    };
                    Collections.sort(result, comp);
                }
                if (sortBy.equals("Rating")) {
                    comp = new Comparator<Movie>() {
                        public int compare(Movie m1, Movie m2) {
                            return Double.compare(m1.vote_average, m2.vote_average);
                        }
                    };
                    Collections.sort(result, comp);
                }
                movieAdapter.clear();
                for (Movie m :result) {
                    movieAdapter.add(m);
                }
                g_view.setAdapter(movieAdapter);
            }
        }
        protected List<Movie> ShowOnlyFavorites(List<Movie> result){
            SharedPreferences sharedPref = getActivity().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String fav_movies_string = sharedPref.getString(getString(R.string.favorite_movies), "");
            List<String> movies= Arrays.asList(fav_movies_string.split(","));
            List<Movie> fav_movies = new ArrayList<Movie>();
            for (Movie m:result){
                if (movies.indexOf(m.title) != -1){
                    fav_movies.add(m);
                }
            }
            return fav_movies;
        }
    }
}
