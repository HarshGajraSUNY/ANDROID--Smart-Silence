package cs580.edu.binghamton.com.smartsilence;

import android.*;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.hssf.record.formula.functions.T;

import java.util.ArrayList;

/**
 * Created by mani on 5/6/17.
 */

public class MyService extends Service implements LocationListener  {

    public static DatabaseReference usersRef =  MainActivity.mRoot.child("TrainingData");

    Location oldLocation, newLocation;
   public static  boolean ISsilentZone = false;
    boolean NoNearby = false;

    public static ArrayList<MyDataModel> TrainingData = new ArrayList<>();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(this,"Created",Toast.LENGTH_LONG).show();

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.getValue() != null) {
                        MyDataModel temp;
                        TrainingData = new ArrayList<>();
                        Map<String, HashMap<String,String>> Dataobj = (Map<String, HashMap<String,String>>) dataSnapshot.getValue();
                        for (Map.Entry<String, HashMap<String,String>> entry : Dataobj.entrySet())
                        {
                            temp = new MyDataModel();
                           HashMap<String,String> myHashData = new HashMap<String, String>();
                            myHashData =  entry.getValue();
                            for(Map.Entry<String,String> innerData : myHashData.entrySet()){

                               if(innerData.getKey() != null && innerData.getValue() != null)
                                if(innerData.getKey().equals("ID"))
                                    temp.ID = innerData.getValue();
                              else  if (innerData.getKey().equals("date"))
                                    temp.date = innerData.getValue();
                                else if(innerData.getKey().equals("time"))
                                    temp.time = innerData.getValue();

                                else if(innerData.getKey().equals("latitude"))

                                    temp.latitude = Double.parseDouble(String.valueOf(innerData.getValue()));

                                else if(innerData.getKey().equals("longitude"))

                                    temp.longitude = Double.parseDouble(String.valueOf(innerData.getValue()));

                            }
                            TrainingData.add(temp);
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"Started",Toast.LENGTH_LONG).show();
        LocationManager myLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return super.onStartCommand(intent, flags, startId);
        }

        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        myLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);

        MyBroadcastReceiver receiver=new MyBroadcastReceiver();
        IntentFilter filter=new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);

        getBaseContext().registerReceiver(receiver,filter);



        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this,"Stopped",Toast.LENGTH_LONG).show();
        super.onDestroy();
    }


    @Override
    public void onLocationChanged(Location location) {

        newLocation = location;
        int count = 0;
        float distance = -1,max = -1;
       String str = "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude();
        MainActivity.t1.setText(str);
        if((MyBroadcastReceiver.RingerMode == 1 || MyBroadcastReceiver.RingerMode == 0))
        {

            MyDataModel obj = new MyDataModel();
            obj.ID = new SimpleDateFormat("M - d - Y, HH:mm:ss ").format(new Date());
            obj.date = new SimpleDateFormat("M - d - Y").format(new Date());

            obj.latitude = location.getLatitude();
            obj.longitude = location.getLongitude();
            obj.time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            Map<String,MyDataModel> map_obj = new HashMap<String, MyDataModel>();
            map_obj.put(obj.ID,obj);
            MyService.usersRef.push().setValue(obj);//setValue(map_obj);
            MyBroadcastReceiver.RingerMode = -1;
        }

        if(TrainingData.size() > 0 && !NoNearby && !ISsilentZone) {
            max = -1;
            while (count < TrainingData.size()) {
                Location temp_location = new Location("");
                float d1 = -1;
                temp_location.setLatitude(TrainingData.get(count).latitude);
                temp_location.setLongitude(TrainingData.get(count).longitude);
                d1 = newLocation.distanceTo(temp_location);
                if (d1 >=0  && d1 < 50) {

                    ISsilentZone = true;
                    NoNearby = false;
                    Toast.makeText(this, "Silent Zone Detected", Toast.LENGTH_LONG).show();

                    break;

                }
                else
                    NoNearby = true;
                if(d1 > max)
                    max = d1;
                count++;
            }
        }


        if(oldLocation != null && NoNearby){

            distance = newLocation.distanceTo(oldLocation);
            if(distance > max){
                NoNearby = false;

            }

        }
        oldLocation = location;

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
