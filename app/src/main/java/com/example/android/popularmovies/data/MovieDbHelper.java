package com.example.android.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
/**
 * Created by devishree on 9/24/15.
 */
public class MovieDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "movies.db";

    public MovieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_MOVIE_TABLE = "CREATE TABLE " +
                MovieContract.MovieEntry.TABLE_NAME + " (" +
                MovieContract.MovieEntry._ID + " INTEGER PRIMARY KEY," +
                MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE + " TEXT NOT NULL, " +
                MovieContract.MovieEntry.COLUMN_OVERVIEW + " TEXT NOT NULL, " +
                MovieContract.MovieEntry.COLUMN_POSTER_PATH + " TEXT, " +
                MovieContract.MovieEntry.COLUMN_RELEASE_DATE + " INTEGER, " +
                MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE + " REAL, " +
                MovieContract.MovieEntry.COLUMN_VOTE_COUNT + " INTEGER, " +
                MovieContract.MovieEntry.COLUMN_POPULARITY + " REAL, " +
                MovieContract.MovieEntry.COLUMN_FAVORITED + " INTEGER " +
                ");";

        final String SQL_CREATE_REVIEW_TABLE = "CREATE TABLE " +
                MovieContract.ReviewEntry.TABLE_NAME + " (" +
                MovieContract.ReviewEntry._ID + " TEXT PRIMARY KEY," +
                MovieContract.ReviewEntry.COLUMN_AUTHOR + " TEXT NOT NULL, " +
                MovieContract.ReviewEntry.COLUMN_CONTENT + " TEXT NOT NULL, " +
                MovieContract.ReviewEntry.COLUMN_LANGUAGE + " TEXT NOT NULL, " +
                MovieContract.ReviewEntry.COLUMN_MOVIE_ID + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + MovieContract.ReviewEntry.COLUMN_MOVIE_ID + ") REFERENCES " +
                MovieContract.MovieEntry.TABLE_NAME + " (" + MovieContract.MovieEntry._ID + "));";

        final String SQL_CREATE_TRAILER_TABLE = "CREATE TABLE " +
                MovieContract.TrailerEntry.TABLE_NAME + " (" +
                MovieContract.TrailerEntry._ID + " TEXT PRIMARY KEY," +
                MovieContract.TrailerEntry.COLUMN_KEY + " TEXT NOT NULL, " +
                MovieContract.TrailerEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                MovieContract.TrailerEntry.COLUMN_LANGUAGE + " TEXT NOT NULL, " +
                MovieContract.TrailerEntry.COLUMN_MOVIE_ID + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + MovieContract.TrailerEntry.COLUMN_MOVIE_ID + ") REFERENCES " +
                MovieContract.MovieEntry.TABLE_NAME + " (" + MovieContract.MovieEntry._ID + "));";

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_REVIEW_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRAILER_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieContract.MovieEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieContract.ReviewEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieContract.TrailerEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

}
