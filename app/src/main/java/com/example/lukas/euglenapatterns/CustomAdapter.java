package com.example.lukas.euglenapatterns;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;


public class CustomAdapter extends BaseAdapter {

    // Context used by LayoutInflater
    private Context mContext;

    // Array of image IDs
    private int [] imageIDs;

    // Layout inflater
    private static LayoutInflater inflater = null;

    // Number of columns in ListView
    private static final int COL = 3;

    // Initializes CustomAdapter
    public CustomAdapter(Context context, int[] images) {
        mContext = context;
        imageIDs = images;
        inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Gets number of rows filled by imageIDs
    @Override
    public int getCount() { return imageIDs.length / COL; }

    // Returns position/itemID of item
    @Override
    public Object getItem(int position) { return position; }

    // Returns itemID/position of item
    @Override
    public long getItemId(int position) { return position; }

    // Class to hold an ImageButton
    public class Holder { ImageButton img; }

    // Gets view when scrolling
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ImageButton img;
        View rowView;
        rowView = inflater.inflate(R.layout.program_list, null);

        // Sets row of ImageButtons
        img = (ImageButton) rowView.findViewById(R.id.image1);
        img.setImageResource(imageIDs[position * COL]);
        img = (ImageButton) rowView.findViewById(R.id.image2);
        img.setImageResource(imageIDs[position * COL + 1]);
        img = (ImageButton) rowView.findViewById(R.id.image3);
        img.setImageResource(imageIDs[position * COL + 2]);

        /*Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.program_list, null);
        holder.img = (ImageButton) rowView.findViewById(R.id.image1);
        holder.img.setImageResource(imageIDs[position*3]);
        holder.img = (ImageButton) rowView.findViewById(R.id.image2);
        holder.img.setImageResource(imageIDs[position*3+1]);
        holder.img = (ImageButton) rowView.findViewById(R.id.image3);
        holder.img.setImageResource(imageIDs[position*3+2]);*/
        /*rowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(context, "You Clicked "+result[position], Toast.LENGTH_LONG).show();
            }
        });*/
        return rowView;
    }
}
