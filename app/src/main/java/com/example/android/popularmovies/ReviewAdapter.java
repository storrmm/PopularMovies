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
public class ReviewAdapter extends CursorAdapter {
    public ReviewAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_review, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String reviewAuthor = cursor.getString(DetailActivityFragment.COL_REVIEW_AUTHOR);
        viewHolder.reviewAuthor.setText(reviewAuthor);
        String reviewContent = cursor.getString(DetailActivityFragment.COL_REVIEW_CONTENT);
        viewHolder.reviewContent.setText(reviewContent);
    }


    private static class ViewHolder {
        public final TextView reviewAuthor;
        public final TextView reviewContent;

        public ViewHolder(View view) {
            reviewAuthor = (TextView) view.findViewById(R.id.list_item_review_author);
            reviewContent = (TextView) view.findViewById(R.id.list_item_review_content);
        }
    }
}
