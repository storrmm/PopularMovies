package com.example.android.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * Created by devishree on 10/2/15.
 */
public class TrailerAdapter extends CursorAdapter {
    public TrailerAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    // These views are reused as needed
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_trailer, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    // Fill-in the views with the contents of the cursor
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String trailerName = cursor.getString(DetailActivityFragment.COL_TRAILER_NAME);
        viewHolder.trailerName.setText(trailerName);
    }

    /**
     * Cache of the children views for a movies grid view.
     */
    private static class ViewHolder {
        public final TextView trailerName;

        public ViewHolder(View view) {
            trailerName = (TextView) view.findViewById(R.id.list_item_trailer_name);
        }
    }
}
