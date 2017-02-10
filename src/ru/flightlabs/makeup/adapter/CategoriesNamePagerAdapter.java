package ru.flightlabs.makeup.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.flightlabs.makeup.CommonI;
import ru.flightlabs.masks.R;

/**
 * Created by sov on 19.11.2016.
 */

public class CategoriesNamePagerAdapter extends PagerAdapter {

    CommonI fdAct;
    Context mContext;
    String[] texts;
    LayoutInflater mLayoutInflater;

    public CategoriesNamePagerAdapter(CommonI context, String[] texts) {
        // something terrible
        mContext = (Context)context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.fdAct = context;
        this.texts = texts;
    }

    @Override
    public int getCount() {
        return texts.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View itemView = mLayoutInflater.inflate(R.layout.category, container, false);
        TextView textView = (TextView) itemView.findViewById(R.id.item_text);
        textView.setText(texts[position]);
        container.addView(itemView);
        itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                fdAct.changeCategory(position);

            }
        });
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }

    @Override
    public float getPageWidth(int position) {
        return 1f / 4;
    }
}