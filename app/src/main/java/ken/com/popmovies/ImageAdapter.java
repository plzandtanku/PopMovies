package ken.com.popmovies;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

// Adapter Class for handling Images
// Also handles managing and storing the Movies we want posters for
public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    // Contains the Movies we want poster images for
    private List<Movie> movies = new ArrayList<Movie>();
    public ImageAdapter(Context c) {
        mContext = c;
    }
    public void add(Movie m){
        movies.add(m);
    }
    public void clear(){
        movies.clear();
    }
    public int getCount() {
        return movies.size();
    }

    public Movie getItem(int position) {

        return movies.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            Picasso.with(mContext).load(movies.get(position).image).into(imageView);
        } else {
            imageView = (ImageView) convertView;
        }
        return imageView;
    }
    public String toString(){
        return movies.toString();
    }
}