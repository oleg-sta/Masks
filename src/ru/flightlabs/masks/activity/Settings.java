package ru.flightlabs.masks.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import ru.flightlabs.masks.R;

public class Settings extends Activity {

    public static boolean debugMode = false;
    public static final String DIRECTORY_SELFIE = "Masks";
    
    public static final String PREFS = "eselfie";
    public static final String DEBUG_MODE = "debugMode";
    public static final String MULTI_MODE = "multiMode";
    public static final String PUPILS_MODE = "pupilsMode";
    public static final String COUNTER_PHOTO = "photoCounter";
    public static final String MODEL_PATH = "modelPath";
    public static final String MODEL_PATH_DEFAULT = "/storage/extSdCard/sp_s2.dat";
    private static final String TAG = "Settings_class";
    public static boolean useLinear;
    public static boolean makeUp;


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
        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
        String path = prefs.getString(Settings.MODEL_PATH, MODEL_PATH_DEFAULT);
        TextView textView = (TextView)findViewById(R.id.file_model);
        textView.setText(path);
    }
    
    public void mailTo(View view) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "feedback@flightlabs.ru", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "eSelfie");
        startActivity(Intent.createChooser(intent, getString(R.string.send_mail_with)));
    }
    
    public void setFile(View view) {
        final SharedPreferences prefs = getSharedPreferences(Settings.PREFS, Context.MODE_PRIVATE);
        String path = prefs.getString(Settings.MODEL_PATH, MODEL_PATH_DEFAULT);
        final EditText input = new EditText(this);
        input.setText(path);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Введи путь модели");
        builder.setView(input).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String newName = input.getText().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Settings.MODEL_PATH, newName);
                editor.commit();
                
            }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.i("DragOverListMen", "No");
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        
    }

}
