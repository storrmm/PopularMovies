package com.example.android.popularmovies.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.android.popularmovies.R;
import com.example.android.popularmovies.data.MovieContract;
import com.example.android.popularmovies.data.MovieProvider;
import com.example.android.popularmovies.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;


public class MoviesSyncAdapter extends AbstractThreadedSyncAdapter {

    public final String LOG_TAG = MoviesSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the TMDB info, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    public static final String SYNC_FINISHED = "SYNC_FINISHED";
    public static final String SYNC_STARTED = "SYNC_STARTED";
    public static final String SEED = "SEED";

    private static final int REQUEST_LIMIT_REACHED_RESPONSE_CODE = 429;
    private static final String REQUEST_LIMIT_REACHED_RETRY_AFTER_HEADER = "Retry-After";
    private static final int REQUEST_LIMIT_REACHED_DEFAULT_WAIT_TIME_IN_SECONDS = 10;
    private static final String TMDB_RESULTS = "results";
    private static final String TMDB_MOVIE_ID = "id";
    private static final String TMDB_MOVIE_ORIGINAL_TITLE = "original_title";
    private static final String TMDB_MOVIE_POSTER_PATH = "poster_path";
    private static final String TMDB_MOVIE_OVERVIEW = "overview";
    private static final String TMDB_MOVIE_VOTE_AVERAGE = "vote_average";
    private static final String TMDB_MOVIE_VOTE_COUNT = "vote_count";
    private static final String TMDB_MOVIE_RELEASE_DATE = "release_date";
    private static final String TMDB_MOVIE_POPULARITY = "popularity";
    private static final String TMDB_REVIEW_ID = "id";
    private static final String TMDB_REVIEW_AUTHOR = "author";
    private static final String TMDB_REVIEW_CONTENT = "content";
    private static final String TMDB_VIDEO_ID = "id";
    private static final String TMDB_VIDEO_KEY = "key";
    private static final String TMDB_VIDEO_NAME = "name";
    private static final String TMDB_VIDEO_SITE = "site";
    private static final String TMDB_VIDEO_TYPE = "type";
    private static final String YOUTUBE_SITE = "youtube";
    private static final String TRAILER_TYPE = "trailer";
    private static final String ID_PLACEHOLDER = "{id}";
    private static final String POPULAR_MOVIES_BASE_URL =
            "http://api.themoviedb.org/3/discover/movie?";
    private static final String POPULAR_MOVIES_REVIEWS_BASE_URL =
            "http://api.themoviedb.org/3/movie/" + ID_PLACEHOLDER + "/reviews?";
    private static final String POPULAR_MOVIES_TRAILERS_BASE_URL =
            "http://api.themoviedb.org/3/movie/" + ID_PLACEHOLDER + "/videos?";
    private static final String TMDB_MOVIE_RELEASE_DATE_FORMAT = "yyyy-MM-dd";

    private SimpleDateFormat TMDB_MOVIE_RELEASE_DATE_SDF =
            new SimpleDateFormat(TMDB_MOVIE_RELEASE_DATE_FORMAT);

    public MoviesSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String apiKey = getContext().getString(R.string.api_key);

        double seed = Math.random();
        Intent startIntent = new Intent(SYNC_STARTED);
        startIntent.putExtra(SEED, seed);
        getContext().sendBroadcast(startIntent);

        try {
            // Construct the URL for the themoviedb.org query (/discover/movie endpoint)
            // http://docs.themoviedb.apiary.io/#reference/discover/discovermovie
            String sortOrder = Utility.getCurrentQueryPreference(getContext());
            // If there is no sort order preference (or sort order is for getting the favorited
            // movies), there is nothing to look up.
            if ((sortOrder == null)||
                    (sortOrder.equals(getContext().getString(R.string.pref_sorting_favorites)))) {
                Log.d(LOG_TAG, "Not calling TMDB due to sortOrder preference value: " + sortOrder);
                return;
            }
            Log.d(LOG_TAG, "Calling TMDB with sortOrder preference value: " + sortOrder);

            URL url = new URL(Uri.parse(POPULAR_MOVIES_BASE_URL).buildUpon()
                    .appendQueryParameter(getContext().getString(R.string.pref_sorting_key), sortOrder)
                    .appendQueryParameter(getContext().getString(R.string.api_param_key), apiKey)
                    .build().toString());

            // Create the request to themoviedb.org, and open the connection
            urlConnection = manageTmdbRequestLimit(url);

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }

            getMoviesDataFromJson(buffer.toString());
            Log.d(LOG_TAG, "TMDB: url : " + url);
            Log.d(LOG_TAG, "Popular Movies Sync correctly ended");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error: " + e.getMessage(), e);
            // If the code didn't successfully get the movie data, there's no point in
            // attempting to parse it.
        } finally {
            Log.d(LOG_TAG, "Finishing Popular Movies Sync");
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException ioe) {
                    Log.e(LOG_TAG, "Error closing stream: " + ioe.getMessage(), ioe);
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            Intent finishIntent = new Intent(SYNC_FINISHED);
            finishIntent.putExtra(SEED, seed);
            getContext().sendBroadcast(finishIntent);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        MoviesSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount,
                context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    /**
     * tries to connect to TMDB API and, in case of request limit reached, retries after needed
     * wait time
     */
    private HttpURLConnection manageTmdbRequestLimit(URL url)
            throws IOException, InterruptedException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();

        int code = urlConnection.getResponseCode();
        if (code==REQUEST_LIMIT_REACHED_RESPONSE_CODE) {
            String retryAfter = urlConnection
                    .getHeaderField(REQUEST_LIMIT_REACHED_RETRY_AFTER_HEADER);
            int waitTimeInSeconds = REQUEST_LIMIT_REACHED_DEFAULT_WAIT_TIME_IN_SECONDS;
            if ((retryAfter!=null) && (!retryAfter.isEmpty())) {
                waitTimeInSeconds = Integer.valueOf(retryAfter) + 1;
            }
            Log.d(LOG_TAG, "TMDB request limit reached. Waiting " + waitTimeInSeconds +
                    " seconds before retrying");

            // Wait
            Thread.sleep(waitTimeInSeconds * 1000);

            // Retry
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
        }

        return urlConnection;
    }

    /**
     * Take the String representing the complete obtained movies data in JSON Format and
     * pull out the needed data
     */
    private void getMoviesDataFromJson(String moviesJsonStr)
            throws JSONException, InterruptedException {

        // Base URL for all poster images
        final String TMDB_POSTER_BASE_URL = "http://image.tmdb.org/t/p/";
        // Image size that is going to be requested
        final String TMDB_IMAGE_SIZE = "w185";

        JSONArray moviesArray = new JSONObject(moviesJsonStr).getJSONArray(TMDB_RESULTS);
        Vector<ContentValues> resultMovies = new Vector<ContentValues>(moviesArray.length());
        Vector<ContentValues> resultReviews = new Vector<ContentValues>();
        Vector<ContentValues> resultTrailers = new Vector<ContentValues>();
        Vector<Integer> resultTmdBIds = new Vector<Integer>();

        for(int i = 0; ((i < moviesArray.length())
                && (i < Integer.valueOf(Utility.MAX_MOVIES))); i++) {
            // Get the JSON object representing the movie
            JSONObject movieResult = moviesArray.getJSONObject(i);

            // If some of the retrieved movies have no id, discard it because it's going
            // to be no possible to do any further needed API call for them
            if (!movieResult.has(TMDB_MOVIE_ID)) {
                continue;
            }

            // Get required movie data, checking for possible missing and null values
            int movieId = movieResult.getInt(TMDB_MOVIE_ID);
            resultTmdBIds.add(movieId);
            String movieOriginalTitle = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_ORIGINAL_TITLE,
                    getContext().getString(R.string.no_original_title_found));
            String posterPath = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_POSTER_PATH, (String)null);
            String movieOverview = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_OVERVIEW,
                    getContext().getString(R.string.no_overview_found));
            Double movieVoteAverage = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_VOTE_AVERAGE, (Double)null);
            Integer movieVoteCount = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_VOTE_COUNT, (Integer)null);
            Date movieReleaseDate = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_RELEASE_DATE, (Date)null);
            Double moviePopularity = checkForMissingOrNullValues(movieResult,
                    TMDB_MOVIE_POPULARITY, (Double)null);

            ContentValues movie = new ContentValues();
            movie.put(MovieContract.MovieEntry._ID, movieId);
            movie.put(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE, movieOriginalTitle);
            movie.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, movieOverview);
            movie.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, (posterPath!=null) ?
                    TMDB_POSTER_BASE_URL + TMDB_IMAGE_SIZE + posterPath : null);
            movie.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, movieVoteAverage);
            movie.put(MovieContract.MovieEntry.COLUMN_VOTE_COUNT, movieVoteCount);
            movie.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, movieReleaseDate.getTime());
            movie.put(MovieContract.MovieEntry.COLUMN_POPULARITY, moviePopularity);

            resultMovies.add(movie);
        }

        // Add to DB
        if (resultMovies.size() > 0 ) {
            ContentValues[] values = new ContentValues[resultMovies.size()];
            resultMovies.toArray(values);
            getContext().getContentResolver()
                    .bulkInsert(MovieContract.MovieEntry.CONTENT_URI, values);
        }

        // Get additional data (reviews and trailers)

        // First, delete all data from a language different than the current one
        getContext().getContentResolver().delete(MovieContract.ReviewEntry.CONTENT_URI,
                MovieProvider.sReviewNotLanguageSelection,
                new String[] {Locale.getDefault().getLanguage()});
        getContext().getContentResolver().delete(MovieContract.TrailerEntry.CONTENT_URI,
                MovieProvider.sTrailerNotLanguageSelection,
                new String[] {Locale.getDefault().getLanguage()});

        for(Integer movieId: resultTmdBIds) {
            resultReviews.addAll(getMovieReviews(movieId));
            resultTrailers.addAll(getMovieTrailers(movieId));
        }

        if (resultReviews.size() > 0 ) {
            ContentValues[] values = new ContentValues[resultReviews.size()];
            resultReviews.toArray(values);
            getContext().getContentResolver()
                    .bulkInsert(MovieContract.ReviewEntry.CONTENT_URI, values);
        }
        if (resultTrailers.size() > 0 ) {
            ContentValues[] values = new ContentValues[resultTrailers.size()];
            resultTrailers.toArray(values);
            getContext().getContentResolver()
                    .bulkInsert(MovieContract.TrailerEntry.CONTENT_URI, values);
        }
    }

    private String checkForMissingOrNullValues(JSONObject jsonObject, String fieldName,
                                               String defaultValue) throws JSONException {
        if ((jsonObject.has(fieldName)) && (!jsonObject.getString(fieldName).equals(""))
                && (!jsonObject.getString(fieldName).equals("null"))){
            return jsonObject.getString(fieldName);
        } else {
            return defaultValue;
        }
    }

    private Double checkForMissingOrNullValues(JSONObject jsonObject, String fieldName,
                                               Double defaultValue) throws JSONException {
        if (jsonObject.has(fieldName)) {
            return jsonObject.getDouble(fieldName);
        } else {
            return defaultValue;
        }
    }

    private Integer checkForMissingOrNullValues(JSONObject jsonObject, String fieldName,
                                               Integer defaultValue) throws JSONException {
        if (jsonObject.has(fieldName)) {
            return jsonObject.getInt(fieldName);
        } else {
            return defaultValue;
        }
    }

    private Date checkForMissingOrNullValues(JSONObject jsonObject, String fieldName,
                                             Date defaultValue) throws JSONException {
        if (jsonObject.has(fieldName)) {
            try {
                return TMDB_MOVIE_RELEASE_DATE_SDF.parse(jsonObject.getString(fieldName));
            } catch (ParseException e) {
                Log.w(LOG_TAG, "Release date could not been correctly parsed");
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    private Vector<ContentValues> getMovieReviews(int movieId) {

        try {
            Vector<ContentValues> reviews = new Vector<ContentValues>();
            String language = Locale.getDefault().getLanguage();

            reviews.addAll(makeMovieReviewsQuery(movieId, language));

            //When the current language is not English, we also get english reviews
            if (!language.equals(Locale.ENGLISH.getLanguage())) {
                reviews.addAll(makeMovieReviewsQuery(movieId, Locale.ENGLISH.getLanguage()));
            }

            return reviews;

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error: " + e.getMessage(), e);
            // If the code didn't successfully get the movie data, there's no point in
            // attempting to parse it.
            return new Vector<ContentValues>();
        }
    }

    private Vector<ContentValues> makeMovieReviewsQuery(int movieId, String language)
            throws InterruptedException, IOException, JSONException {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String apiKey = getContext().getString(R.string.api_key);

        try {
            // Construct the URL for the themoviedb.org query (/movie/{id}/reviews endpoint)
            URL url = new URL(Uri.parse(
                    POPULAR_MOVIES_REVIEWS_BASE_URL
                            .replace(ID_PLACEHOLDER, Integer.valueOf(movieId).toString()))
                    .buildUpon()
                    .appendQueryParameter(getContext().getString(R.string.api_param_key), apiKey)
                    .build().toString());

            // Create the request to themoviedb.org, and open the connection
            urlConnection = manageTmdbRequestLimit(url);

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return new Vector<ContentValues>();
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return new Vector<ContentValues>();
            }

            return getMovieReviewsDataFromJson(buffer.toString(), movieId, language);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException ioe) {
                    Log.e(LOG_TAG, "Error closing stream: " + ioe.getMessage(), ioe);
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private Vector<ContentValues> getMovieReviewsDataFromJson(String reviewsJsonStr, int movieId,
                                                              String language)
            throws JSONException {
        JSONArray reviewsArray = new JSONObject(reviewsJsonStr).getJSONArray(TMDB_RESULTS);
        Vector<ContentValues> resultReviews = new Vector<ContentValues>(reviewsArray.length());

        for(int i = 0; i < reviewsArray.length(); i++) {
            // Get the JSON object representing the review
            JSONObject reviewResult = reviewsArray.getJSONObject(i);

            // If some of the retrieved reviews have no id, discard it because it's going
            // to be no possible to do any further needed API call for them
            if (!reviewResult.has(TMDB_REVIEW_ID)) {
                continue;
            }

            // Get required review data, checking for possible missing and null values
            String reviewId = reviewResult.getString(TMDB_REVIEW_ID);
            String reviewAuthor = checkForMissingOrNullValues(reviewResult,
                    TMDB_REVIEW_AUTHOR,
                    getContext().getString(R.string.no_author_found));
            String reviewContent = checkForMissingOrNullValues(reviewResult,
                    TMDB_REVIEW_CONTENT, "");

            ContentValues review = new ContentValues();
            review.put(MovieContract.ReviewEntry._ID, reviewId);
            review.put(MovieContract.ReviewEntry.COLUMN_AUTHOR, reviewAuthor);
            review.put(MovieContract.ReviewEntry.COLUMN_CONTENT, reviewContent);
            review.put(MovieContract.ReviewEntry.COLUMN_LANGUAGE, language);
            review.put(MovieContract.ReviewEntry.COLUMN_MOVIE_ID, movieId);

            resultReviews.add(review);
        }

        return resultReviews;
    }

    private Vector<ContentValues> getMovieTrailers(int movieId) {

        try {
            Vector<ContentValues> trailers = new Vector<ContentValues>();
            String language = Locale.getDefault().getLanguage();

            trailers.addAll(makeMovieTrailersQuery(movieId, language));

            //When the current language is not English, we also get english reviews
            if (!language.equals(Locale.ENGLISH.getLanguage())) {
                trailers.addAll(makeMovieTrailersQuery(movieId, Locale.ENGLISH.getLanguage()));
            }

            return trailers;

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error: " + e.getMessage(), e);
            // If the code didn't successfully get the movie data, there's no point in
            // attempting to parse it.
            return new Vector<ContentValues>();
        }
    }

    private Vector<ContentValues> makeMovieTrailersQuery(int movieId, String language)
            throws InterruptedException, IOException, JSONException {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String apiKey = getContext().getString(R.string.api_key);

        try {
            // Construct the URL for the themoviedb.org query (/movie/{id}/trailers endpoint)
            URL url = new URL(Uri.parse(
                    POPULAR_MOVIES_TRAILERS_BASE_URL
                            .replace(ID_PLACEHOLDER, Integer.valueOf(movieId).toString()))
                    .buildUpon()
                    .appendQueryParameter(getContext().getString(R.string.api_param_key), apiKey)
                    .build().toString());

            // Create the request to themoviedb.org, and open the connection
            urlConnection = manageTmdbRequestLimit(url);

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return new Vector<ContentValues>();
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return new Vector<ContentValues>();
            }

            return getMovieTrailersDataFromJson(buffer.toString(), movieId, language);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException ioe) {
                    Log.e(LOG_TAG, "Error closing stream: " + ioe.getMessage(), ioe);
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private Vector<ContentValues> getMovieTrailersDataFromJson(String reviewsJsonStr, int movieId,
                                                               String language)
            throws JSONException {
        JSONArray trailersArray = new JSONObject(reviewsJsonStr).getJSONArray(TMDB_RESULTS);
        Vector<ContentValues> resultTrailers = new Vector<ContentValues>(trailersArray.length());

        for(int i = 0; i < trailersArray.length(); i++) {
            // Get the JSON object representing the review
            JSONObject trailerResult = trailersArray.getJSONObject(i);

            // If some of the retrieved reviews have no id, discard it because it's going
            // to be no possible to do any further needed API call for them
            if (!trailerResult.has(TMDB_REVIEW_ID)) {
                continue;
            }

            // Get required review data, checking for possible missing and null values
            String trailerId = trailerResult.getString(TMDB_VIDEO_ID);
            String trailerKey = checkForMissingOrNullValues(trailerResult,
                    TMDB_VIDEO_KEY, (String) null);
            String trailerName = checkForMissingOrNullValues(trailerResult,
                    TMDB_VIDEO_NAME, getContext().getString(R.string.no_trailer_name_found));
            String trailerSite = checkForMissingOrNullValues(trailerResult,
                    TMDB_VIDEO_SITE, (String) null);
            String trailerType = checkForMissingOrNullValues(trailerResult,
                    TMDB_VIDEO_TYPE, (String) null);

            // Popular Movies app will ignore any video from a site different than "YouTube" and
            // with a type different from "Trailer"
            if ((trailerSite==null) || (!trailerSite.toLowerCase().equals(YOUTUBE_SITE)) ||
                    (trailerType==null) || (!trailerType.toLowerCase().equals(TRAILER_TYPE))) {
                continue;
            }

            ContentValues trailer = new ContentValues();
            trailer.put(MovieContract.TrailerEntry._ID, trailerId);
            trailer.put(MovieContract.TrailerEntry.COLUMN_KEY, trailerKey);
            trailer.put(MovieContract.TrailerEntry.COLUMN_NAME, trailerName);
            trailer.put(MovieContract.TrailerEntry.COLUMN_LANGUAGE, language);
            trailer.put(MovieContract.TrailerEntry.COLUMN_MOVIE_ID, movieId);

            resultTrailers.add(trailer);
        }

        return resultTrailers;
    }
}