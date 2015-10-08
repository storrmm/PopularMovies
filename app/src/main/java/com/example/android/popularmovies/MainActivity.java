package com.example.android.popularmovies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.popularmovies.sync.MoviesSyncAdapter;


public class MainActivity extends AppCompatActivity
        implements PostersFragment.Callback, DetailActivityFragment.Callback {

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private boolean mTwoPane;
    private double mSyncSeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (findViewById(R.id.detail_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                DetailActivityFragment detailFragment = new DetailActivityFragment();
                Bundle arguments = new Bundle();
                arguments.putBoolean(DetailActivityFragment.IS_TWO_PANE, mTwoPane);
                detailFragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container, detailFragment,
                                DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            if (getSupportActionBar()!=null) {
                getSupportActionBar().setElevation(0f);
            }
        }

        PostersFragment moviesFragment = ((PostersFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_container));
        moviesFragment.setUseTwoPaneLayout(mTwoPane);

        MoviesSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(syncStartedReceiver,
                new IntentFilter(MoviesSyncAdapter.SYNC_STARTED));
        registerReceiver(syncFinishedReceiver,
                new IntentFilter(MoviesSyncAdapter.SYNC_FINISHED));
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(syncStartedReceiver);
        unregisterReceiver(syncFinishedReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri dateUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (dateUri==null) {
                DetailActivityFragment detailFragment =
                        ((DetailActivityFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.detail_container));
                detailFragment.hideDetailLayout();
            }
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, dateUri);
            DetailActivityFragment fragment = new DetailActivityFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity .class)
                    .setData(dateUri);
            startActivity(intent);
        }
    }

    @Override
    public void onTrailerItemSelected(String url) {
        Utility.openMovieTrailer(this, url);
    }

    private BroadcastReceiver syncStartedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mSyncSeed = intent.getDoubleExtra(MoviesSyncAdapter.SEED, 0);
            PostersFragment moviesFragment = ((PostersFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.main_container));
        }
    };

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            double finishSeed = intent.getDoubleExtra(MoviesSyncAdapter.SEED, 0);
            if (finishSeed==mSyncSeed) {
                PostersFragment moviesFragment = ((PostersFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.main_container));
            }
        }
    };
}
