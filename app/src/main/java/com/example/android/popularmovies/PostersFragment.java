package com.example.android.popularmovies;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.popularmovies.data.MovieContract;
import com.example.android.popularmovies.data.MovieProvider;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class PostersFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String QUERY_PREFERENCE_KEY = "QUERY_PREFERENCE";
    private static final String SELECTED_POSITION_KEY = "SELECTED_POSITION";
    private static final int MOVIE_LOADER_ID = 0;

    public static final int MAX_PAGES = 50;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_POSTER_PATH
    };
    public static final int COL_ID = 0;
    public static final int COL_POSTER_PATH = 1;

    private com.example.android.popularmovies.MovieAdapter mMovieAdapter;
    private String mQueryPreference;
    private boolean mUseTwoPaneLayout;
    private boolean mPreferenceHasChanged;
    private int mSelectedPosition;

    @Bind(R.id.gridview_movies) GridView mGridView;
    @Bind(R.id.loading_text_view) TextView mLoadingTextView;

    public PostersFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mSelectedPosition != ListView.INVALID_POSITION) {
            savedInstanceState.putInt(SELECTED_POSITION_KEY, mSelectedPosition);
        }
        savedInstanceState.putString(QUERY_PREFERENCE_KEY, mQueryPreference);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(Boolean.TRUE);

        mQueryPreference = null;
        mSelectedPosition = ListView.INVALID_POSITION;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(QUERY_PREFERENCE_KEY)) {
                mQueryPreference = savedInstanceState.getString(QUERY_PREFERENCE_KEY);
            }
            if (savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
                mSelectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
            }
        }
        getLoaderManager().initLoader(MOVIE_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        mMovieAdapter = new com.example.android.popularmovies.MovieAdapter(getActivity(), null, 0);
        mGridView.setAdapter(mMovieAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int i, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(i);
                if (cursor != null) {
                    Uri uri = null;
                    if (cursor.getCount() > 0) {
                        uri = MovieContract.MovieEntry
                                .buildMovieUri(cursor.getInt(COL_ID));
                    }
                    ((Callback) getActivity())
                            .onItemSelected(uri);
                }
                mSelectedPosition = i;
            }
        });

//        mSwipeLayout.setOnRefreshListener(this);
//        mSwipeLayout.setColorSchemeResources(R.color.primary, R.color.accent, R.color.primary_dark);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        mPreferenceHasChanged = Boolean.FALSE;
        String currentQueryPreference = Utility.getCurrentQueryPreference(getActivity());

        if ((!currentQueryPreference.equals(mQueryPreference))) {
            mPreferenceHasChanged = Boolean.TRUE;
            mQueryPreference = currentQueryPreference;
            Utility.updateMoviesInfo(this, new int[]{MOVIE_LOADER_ID}, this);
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences sharedPref = getActivity().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void setUseTwoPaneLayout(boolean useTwoPaneLayout) {
        mUseTwoPaneLayout = useTwoPaneLayout;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri moviesUri = MovieContract.MovieEntry.CONTENT_URI;
        String currentQueryPreference = Utility.getCurrentQueryPreference(getActivity());
        String sortOrder;
        String selection = null;
        String[] selectionArgs = null;
        if (currentQueryPreference.equals(getString(R.string.pref_sorting_favorites))) {
            //Popular Movies will show favorited movies sorted by popularity
            sortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY + " DESC";
            selection = MovieProvider.sFavoritedMoviesSelection;
            selectionArgs = new String[]{"1"};
        } else if (currentQueryPreference.equals(getString(R.string.pref_sorting_rating))) {
            sortOrder = MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE + " DESC";
        } else { // Popularity sort order is also the default used preference
            sortOrder = MovieContract.MovieEntry.COLUMN_POPULARITY + " DESC";
        }

        return new CursorLoader(getActivity(), moviesUri, MOVIE_COLUMNS, selection,
                selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //mSwipeLayout.setRefreshing(false);
        mMovieAdapter.swapCursor(data);
        if (data.getCount()>0) {
            PostersFragment.this.mGridView.setVisibility(View.VISIBLE);
            PostersFragment.this.mLoadingTextView.setVisibility(View.INVISIBLE);
            int position = mSelectedPosition;
            if (position==ListView.INVALID_POSITION) {
                position = 0;
            }
            if ((mPreferenceHasChanged) && (mUseTwoPaneLayout)) {
                CustomRunnable customRunnable = new CustomRunnable(position);
                mGridView.postDelayed(customRunnable, 0);
            }
            mGridView.smoothScrollToPosition(position);
        } else {
            //Movies data cannot be retrieved from DB
            PostersFragment.this.mGridView.setVisibility(View.INVISIBLE);
            PostersFragment.this.mLoadingTextView.setVisibility(View.VISIBLE);
            if (mUseTwoPaneLayout) {
                CustomRunnable customRunnable = new CustomRunnable(0);
                mGridView.postDelayed(customRunnable, 0);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMovieAdapter.swapCursor(null);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public interface Callback {
        void onItemSelected(Uri dateUri);
    }


    private class CustomRunnable implements Runnable {

        private int position;

        public CustomRunnable(int position) {
            this.position = position;
        }

        @Override
        public void run() {
            mGridView.setSoundEffectsEnabled(Boolean.FALSE);
            mGridView.performItemClick(mGridView.getChildAt(position),
                    position, mGridView.getItemIdAtPosition(position));
            mGridView.setSoundEffectsEnabled(Boolean.TRUE);
        }
    }
}