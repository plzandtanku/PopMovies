package ken.com.popmovies;

/**
 *     Defines the Movie object, which contains:
 *
 *     title,image,overview,vote_average,release_date,popularity,id, and the JSON string
 */
public class Movie {
    String title;
    String image;
    String overview;
    Double vote_average;
    String release_date;
    String json;
    Double popularity;
    Integer id;
    public Movie(String title,String image,
                 String overview,Double vote_average,
                 String release_date,String json,
                 Double popularity, Integer id){
        this.title = title;
        this.image = image;
        this.overview = overview;
        this.vote_average = vote_average;
        this.release_date = release_date;
        this.json = json;
        this.popularity = popularity;
        this.id = id;
    }
    public String toString(){
        return String.format("%s - %.2f - %.2f",title,vote_average,popularity);
    }
}
