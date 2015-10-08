package com.example.android.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
/**
 * Created by devishree on 10/1/15.
 */
public class MovieAdapter extends CursorAdapter {

    public MovieAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item_movie, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String posterPath = cursor.getString(PostersFragment.COL_POSTER_PATH);
        if (posterPath!=null) {
            Picasso.with(mContext).load(posterPath).into(viewHolder.posterView);
        } else {
            Picasso.with(mContext).load(R.drawable.user_placeholder_error)
                    .into(viewHolder.posterView);
        }
    }

    private static class ViewHolder {
        public final ImageView posterView;

        public ViewHolder(View view) {
            posterView = (ImageView) view.findViewById(R.id.grid_item_movie_poster);
        }
    }
}
