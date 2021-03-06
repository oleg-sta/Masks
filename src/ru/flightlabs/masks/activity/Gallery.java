package ru.flightlabs.masks.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import ru.flightlabs.commonlib.Settings;
import ru.flightlabs.masks.adapter.PhotoPagerAdapter;
import ru.flightlabs.masks.R;

public class Gallery extends Activity {
    
    private static final String TAG = "Gallery_class";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
        final Activity d = this;
        
        final ViewPager pager = (ViewPager)findViewById(R.id.pager);
        // TODO список
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File newFile = new File(file, Settings.DIRECTORY_SELFIE);
        if(!newFile.exists()){
            newFile.mkdirs();
        }
        File[] files = newFile.listFiles();

        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return -Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        
        final List<String> photos = new ArrayList<String>();
        for (File f : files) {
            photos.add(f.getPath());
        }
        final PhotoPagerAdapter adapter = new PhotoPagerAdapter(this, photos);
        pager.setAdapter(adapter);
        
        findViewById(R.id.thrash_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(d);
                builder.setTitle(R.string.delete_photo);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new File(adapter.photos.get(pager.getCurrentItem())).delete();
                        photos.remove(adapter.photos.get(pager.getCurrentItem()));
                        adapter.notifyDataSetChanged();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }
        });
        
        findViewById(R.id.share_button).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO проверить доступ, может надо переложить файла MediaStore или ContentProvider
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(adapter.photos.get(pager.getCurrentItem()))));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.send_photo_with)));
            }
        });

        findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

}