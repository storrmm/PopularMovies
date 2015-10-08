package com.example.android.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.util.Log;

import com.example.android.popularmovies.sync.MoviesSyncAdapter;

/**
 * Created by devishree on 9/29/15.
 */
public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();

    public static final String MAX_MOVIES = "100";
    public static String getCurrentQueryPreference(Context context) {
        //Get sort_order preference value. By default, use sort by popularity option
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        return preferences.getString(context.getString(R.string.pref_sorting_key),
                context.getString(R.string.pref_sorting_default));
    }

    public static void openMovieTrailer(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + url + ", no receiving apps installed!");
        }
    }

    public static void updateMoviesInfo(Fragment fragment, int[] loaderIds,
                                        LoaderManager.LoaderCallbacks callbacks) {
        for (int loaderId : loaderIds) {
            fragment.getLoaderManager().restartLoader(loaderId, null, callbacks);
        }
        MoviesSyncAdapter.syncImmediately(fragment.getActivity());
    }
}
