package ru.flightlabs.masks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class Settings extends Activity {
    
    public static final String PREFS = "eselfie";
    public static final String DEBUG_MODE = "debugMode";
    public static final String MULTI_MODE = "multiMode";
    public static final String PUPILS_MODE = "pupilsMode";
    public static final String COUNTER_PHOTO = "photoCounter";
    private static final String TAG = "Settings_class";

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                
            }
        });
    }
    
    public void mailTo(View view) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "feedback@flightlabs.ru", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "eSelfie");
        startActivity(Intent.createChooser(intent, getString(R.string.send_mail_with)));
    }

}
