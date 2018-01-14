package cs580.edu.binghamton.com.smartsilence;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mani on 5/6/17.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {

    String str = "";
    public static int RingerMode = -1;
    @Override
    public void onReceive(Context context, Intent intent) {
        AudioManager myAudioManager;
        myAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        RingerMode =  myAudioManager.getRingerMode();

            if(RingerMode == 0 || RingerMode == 1){

                Toast.makeText(context,"In Silent Mode",Toast.LENGTH_LONG).show();
               // Toast.makeText(context,str,Toast.LENGTH_LONG).show();

            }
    }
}
