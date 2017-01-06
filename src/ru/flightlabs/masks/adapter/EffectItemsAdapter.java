package ru.flightlabs.masks.adapter;

import android.app.Activity;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import ru.flightlabs.masks.R;

public class EffectItemsAdapter extends BaseAdapter {
    
    Activity context;
    TypedArray images;
    
    public EffectItemsAdapter(Activity context, TypedArray images) {
        this.context = context;
        this.images = images;
    }

    @Override
    public int getCount() {
        return images.length();
    }

    @Override
    public Object getItem(int position) {
        return images.getResourceId(position, 0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        convertView = inflater.inflate(R.layout.item_effect, null, true);
        ImageView im = (ImageView)convertView.findViewById(R.id.item_image);
        im.setImageResource(images.getResourceId(position, 0));
        return convertView;
    }

   

}
