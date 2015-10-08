package com.example.android.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.example.android.popularmovies.Utility;

/**
 * Created by devishree on 9/25/15.
 */
public class MovieProvider extends ContentProvider {

    //"movie._id = ?" selection String
    public static final String sMovieSelection = MovieContract.MovieEntry.TABLE_NAME + "." +
            MovieContract.MovieEntry._ID + " = ? ";
    //"movie.favorited = ?" selection String
    public static final String sFavoritedMoviesSelection = MovieContract.MovieEntry.TABLE_NAME +
            "." + MovieContract.MovieEntry.COLUMN_FAVORITED + " = ? ";
    //"review._id = ?" selection String
    public static final String sReviewSelection = MovieContract.ReviewEntry.TABLE_NAME + "." +
            MovieContract.ReviewEntry._ID + " = ? ";
    //"trailer._id = ?" selection String
    public static final String sTrailerSelection = MovieContract.TrailerEntry.TABLE_NAME + "." +
            MovieContract.TrailerEntry._ID + " = ? ";
    //"review.movie_id = ?" selection String
    public static final String sReviewByMovieIdSelection = MovieContract.ReviewEntry.TABLE_NAME +
            "." + MovieContract.ReviewEntry.COLUMN_MOVIE_ID + " = ? ";
    //"trailer.movie_id = ?" selection String
    public static final String sTrailerByMovieIdSelection = MovieContract.TrailerEntry.TABLE_NAME +
            "." + MovieContract.TrailerEntry.COLUMN_MOVIE_ID + " = ? ";
    //"review.language <> ?" selection String
    public static final String sReviewNotLanguageSelection = MovieContract.ReviewEntry.TABLE_NAME + "." +
            MovieContract.ReviewEntry.COLUMN_LANGUAGE + " <> ? ";
    //"trailer.language <> = ?" selection String
    public static final String sTrailerNotLanguageSelection = MovieContract.TrailerEntry.TABLE_NAME + "." +
            MovieContract.TrailerEntry.COLUMN_LANGUAGE + " <> ? ";

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sMovieQueryBuilder;
    private static final SQLiteQueryBuilder sReviewQueryBuilder;
    private static final SQLiteQueryBuilder sTrailerQueryBuilder;
    private static final int MOVIES = 100;
    private static final int MOVIE_WITH_ID = 101;
    private static final int REVIEWS = 200;
    private static final int REVIEWS_WITH_ID = 201;
    private static final int TRAILERS = 300;
    private static final int TRAILERS_WITH_ID = 301;

    private MovieDbHelper mOpenHelper;

    static{
        sMovieQueryBuilder = new SQLiteQueryBuilder();
        sMovieQueryBuilder.setTables(MovieContract.MovieEntry.TABLE_NAME);
        sReviewQueryBuilder = new SQLiteQueryBuilder();
        sReviewQueryBuilder.setTables(MovieContract.ReviewEntry.TABLE_NAME);
        sTrailerQueryBuilder = new SQLiteQueryBuilder();
        sTrailerQueryBuilder.setTables(MovieContract.TrailerEntry.TABLE_NAME);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIES:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE_WITH_ID:
                return MovieContract.MovieEntry.CONTENT_ITEM_TYPE;
            case REVIEWS:
                return MovieContract.ReviewEntry.CONTENT_TYPE;
            case REVIEWS_WITH_ID:
                return MovieContract.ReviewEntry.CONTENT_ITEM_TYPE;
            case TRAILERS:
                return MovieContract.TrailerEntry.CONTENT_TYPE;
            case TRAILERS_WITH_ID:
                return MovieContract.TrailerEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "movies/*"
            case MOVIE_WITH_ID:
            {
                retCursor = getMovieById(uri, projection, sortOrder);
                break;
            }
            // "movies"
            case MOVIES: {
                retCursor = getMovies(projection, selection, selectionArgs, sortOrder);
                break;
            }
            // "reviews/*"
            case REVIEWS_WITH_ID:
            {
                retCursor = getReviewsByMovieId(uri, projection, sortOrder);
                break;
            }
            // "reviews"
            case REVIEWS: {
                retCursor = getReviews(projection, selection, selectionArgs, sortOrder);
                break;
            }
            // "trailers/*"
            case TRAILERS_WITH_ID:
            {
                retCursor = getTrailersByMovieId(uri, projection, sortOrder);
                break;
            }
            // "trailers"
            case TRAILERS: {
                retCursor = getTrailers(projection, selection, selectionArgs, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case MOVIES: {
                long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = MovieContract.MovieEntry.buildMovieUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case REVIEWS: {
                long _id = db.insert(MovieContract.ReviewEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = MovieContract.ReviewEntry.buildReviewsUriByMovieId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case TRAILERS: {
                long _id = db.insert(MovieContract.TrailerEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = MovieContract.TrailerEntry.buildTrailersUriByMovieId(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case MOVIES:
                rowsDeleted = db.delete(MovieContract.MovieEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            case REVIEWS:
                rowsDeleted = db.delete(MovieContract.ReviewEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            case TRAILERS:
                rowsDeleted = db.delete(MovieContract.TrailerEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case MOVIES:
                rowsUpdated = db.update(MovieContract.MovieEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case REVIEWS:
                rowsUpdated = db.update(MovieContract.ReviewEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case TRAILERS:
                rowsUpdated = db.update(MovieContract.TrailerEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIES: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        // First try to update possible existing value in DB
                        int updated = db.update(MovieContract.MovieEntry.TABLE_NAME, value,
                                sMovieSelection,
                                new String[]{value.getAsString(MovieContract.MovieEntry._ID)});
                        if (updated == 1) {
                            returnCount++;
                        } else {
                            long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            case REVIEWS: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        // First try to update possible existing value in DB
                        int updated = db.update(MovieContract.ReviewEntry.TABLE_NAME, value,
                                sReviewSelection,
                                new String[]{value.getAsString(MovieContract.ReviewEntry._ID)});
                        if (updated == 1) {
                            returnCount++;
                        } else {
                            long _id = db.insert(MovieContract.ReviewEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            case TRAILERS: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        // First try to update possible existing value in DB
                        int updated = db.update(MovieContract.TrailerEntry.TABLE_NAME, value,
                                sTrailerSelection,
                                new String[]{value.getAsString(MovieContract.TrailerEntry._ID)});
                        if (updated == 1) {
                            returnCount++;
                        } else {
                            long _id = db.insert(MovieContract.TrailerEntry.TABLE_NAME, null, value);
                            if (_id != -1) {
                                returnCount++;
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

    private Cursor getMovies(String[] projection, String selection, String[] selectionArgs,
                             String sortOrder) {
        return sMovieQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
                Utility.MAX_MOVIES
        );
    }

    private Cursor getMovieById(Uri uri, String[] projection, String sortOrder) {
        String movieId = MovieContract.MovieEntry.getMovieIdFromUri(uri);
        return sMovieQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sMovieSelection,
                new String[]{movieId},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getReviews(String[] projection, String selection, String[] selectionArgs,
                              String sortOrder) {
        return sReviewQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getReviewsByMovieId(Uri uri, String[] projection, String sortOrder) {
        String movieId = MovieContract.ReviewEntry.getMovieIdFromUri(uri);
        return sReviewQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sReviewByMovieIdSelection,
                new String[]{movieId},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getTrailers(String[] projection, String selection, String[] selectionArgs,
                               String sortOrder) {
        return sTrailerQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getTrailersByMovieId(Uri uri, String[] projection, String sortOrder) {
        String movieId = MovieContract.ReviewEntry.getMovieIdFromUri(uri);
        return sTrailerQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sTrailerByMovieIdSelection,
                new String[]{movieId},
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MovieContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, MovieContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, MovieContract.PATH_MOVIES + "/#", MOVIE_WITH_ID);
        matcher.addURI(authority, MovieContract.PATH_REVIEWS, REVIEWS);
        matcher.addURI(authority, MovieContract.PATH_REVIEWS + "/#", REVIEWS_WITH_ID);
        matcher.addURI(authority, MovieContract.PATH_TRAILERS, TRAILERS);
        matcher.addURI(authority, MovieContract.PATH_TRAILERS + "/#", TRAILERS_WITH_ID);

        return matcher;
    }
}