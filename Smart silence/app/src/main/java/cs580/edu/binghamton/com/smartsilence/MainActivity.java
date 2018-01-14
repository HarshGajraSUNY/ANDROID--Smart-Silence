package cs580.edu.binghamton.com.smartsilence;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.EditText;
import android.app.NotificationManager;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.WindowManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.String;



//comments

public class MainActivity extends AppCompatActivity {

    public static TextView t1;
    public AudioManager myAM;
    TextView t2,txtstd_x,txtstd_y,txtstd_z;
    DescriptiveStatistics   stats_x,stats_y,stats_z; //= new DescriptiveStatistics();

    TextView textX, textY, textZ;
    SensorManager sensorManager;
    Sensor sensor;
    Button strt,stop,button1;
    EditText txtActivity;
    File path,file;
    FileWriter writer;
    Workbook wb;
    Sheet sheet;
    Row row_mic;
    Cell c_mic;
    int row_mic_count =0;
    int count = 0,index= 0;
    Boolean start = false;
    double accel_x[];
    double accel_y[];
    double accel_z[];

    double avg_x,avg_y,avg_z;
    double std_x,std_y,std_z;
    int Confidence = 0;
    int PERMISSION_REQUEST = 0;
    public static final String PREFS_NAME = "MyPrefsFile";
    NotificationManager notificationManager;
    protected SoundMeter meter;
    protected double oldAverageAmplitude = 0;
    protected double averageAmplitude = 0;
    protected double totalAmplitude = 0;
    protected long amplitudeCount = 0;
    protected double previousAmplitude = 0;
    protected double currentAmplitude;
    protected int refreshRate = 250;
    Sheet sheet2;

    public static  DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this,MyService.class);
        startService(intent);
        path = Environment.getExternalStoragePublicDirectory
                (
                        //Environment.DIRECTORY_PICTURES
                        Environment.DIRECTORY_DOWNLOADS
                );
        button1 = (Button)findViewById(R.id.btnstrt);
        strt = (Button)findViewById(R.id.start);
        myAM = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        stop = (Button)findViewById(R.id.stop);
        txtActivity = (EditText)findViewById(R.id.activity);
        stats_x = new DescriptiveStatistics();
        stats_y = new DescriptiveStatistics();
        stats_z = new DescriptiveStatistics();
        wb = new HSSFWorkbook();
        accel_x = new double[200];
        accel_y = new double[200];
        accel_z = new double[200];
        sheet = wb.createSheet("Test - 1");
        sheet2 = wb.createSheet("Microphone");
         notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        int permissionNotifications = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission. ACCESS_NOTIFICATION_POLICY);

        if (permissionNotifications != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.ACCESS_NOTIFICATION_POLICY },
                    PERMISSION_REQUEST
            );
        }

        file = new File(path, "ActivtyData.xlsx");
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final TextView txt = (TextView) findViewById(R.id.lbl);
        t1 = (TextView) findViewById(R.id.textView);
        txtstd_x = (TextView) findViewById(R.id.textView2);
        txtstd_y = (TextView) findViewById(R.id.textView3);
        txtstd_z = (TextView) findViewById(R.id.textView4);
        // t2 = (TextView) findViewById(R.id.textView2);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        textX = (TextView) findViewById(R.id.textX);
        textY = (TextView) findViewById(R.id.textY);
        textZ = (TextView) findViewById(R.id.textZ);

        strt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(txtActivity.getText().length()  != 0)
                    start = true;

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                start = false;

            }
        });

        if (meter == null)
        {
            meter = new SoundMeter(); //instance
            meter.start();
        }

        final Handler h = new Handler();

        h.postDelayed(new Runnable()
        {
            public void run()
            {
                //do something
                h.postDelayed(this, 500);
                AsyncMeter asMeter = new AsyncMeter();
                asMeter.execute();
            }
        }, refreshRate);

    }
    SensorEventListener accelListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            textX.setText("X Axis : " +  x);
            textY.setText("Y Axis : " +  y);
            textZ.setText("Z Axis: "  +  z);

            Cell c = null;
            Row row = null;

            if(count < 200 && (start || MyService.ISsilentZone)) {


                if (count == 0) {

                    Toast.makeText(getBaseContext(), "Data samples Collection started", Toast.LENGTH_LONG).show();

                    row = sheet.createRow(count);
                    c = row.createCell(0);
                    c.setCellValue("Location");
                    c = row.createCell(1);
                    c.setCellValue(t1.getText().toString());
                    count++;
                    row = sheet.createRow(count);
                    c = row.createCell(0);
                    c = row.createCell(1);
                    c.setCellValue("Activity -" + txtActivity.getText().toString());
                    count++;
                    c = row.createCell(0);
                    row = sheet.createRow(count);
                    count++;
                    c = row.createCell(0);
                    c.setCellValue("Time");
                    c = row.createCell(1);
                    c.setCellValue("X-Axis");
                    c = row.createCell(2);
                    c.setCellValue("Y-Axis");
                    c = row.createCell(3);
                    c.setCellValue("Z-Axis");

                }

                accel_x[index] = x;
                accel_y[index] = y;
                accel_z[index] = z;
                index++;

                stats_x.addValue(x);
                stats_y.addValue(y);
                stats_z.addValue(z);

                row = sheet.createRow(count);
                c = row.createCell(0);
                String currentTimeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
                c.setCellValue(currentTimeString);
                c = row.createCell(1);
                c.setCellValue(x);
                c = row.createCell(2);
                c.setCellValue(y);
                c = row.createCell(3);
                c.setCellValue(z);

                try {
                    FileOutputStream os = new FileOutputStream(file);
                    wb.write(os);
                    count++;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if(count == 200){

                Toast.makeText(getBaseContext(), "200 data samples Captured", Toast.LENGTH_LONG).show();


                std_x = stats_x.getStandardDeviation();
                std_y = stats_y.getStandardDeviation();
                std_z = stats_z.getStandardDeviation();


                System.out.println("X avg " + avg_x);
                System.out.println("Y avg " + avg_y);
                System.out.println("Z avg " + avg_z);

                System.out.println("X std " + std_x);
                System.out.println("Y std " + std_y);
                System.out.println("Z std"  + std_z);



                if((std_x > -0.5 && std_x < 0.5) && (std_y > -0.5 && std_y < 0.5) && (std_z > -0.5 && std_z < 0.5)){

                    Toast.makeText(getBaseContext(), "User is not moving: " + std_x, Toast.LENGTH_LONG).show();
                    Confidence++;
                    if(Confidence >= 2 && MyService.ISsilentZone && ConvertAmplitudeToDecibel(averageAmplitude) > 0 && ConvertAmplitudeToDecibel(averageAmplitude) < 75 ){
                        myAM.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        Toast.makeText(getBaseContext(), "Mobile triggered to Silent mode - " + ConvertAmplitudeToDecibel(averageAmplitude), Toast.LENGTH_LONG).show();

                    }

                }

                else
                {

                    Toast.makeText(getBaseContext(), "User is moving: " + std_x, Toast.LENGTH_LONG).show();
                    Confidence = 0;
                }
                Toast.makeText(getBaseContext(), "Current Confidence: "+Confidence, Toast.LENGTH_LONG).show();

                txtstd_x.setText(String.valueOf(std_x));
                txtstd_y.setText(String.valueOf(std_y));
                txtstd_z.setText(String.valueOf(std_z));
                count++;
                row = sheet.createRow(count);
                c = row.createCell(0);
                String currentTimeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
                c.setCellValue(currentTimeString);
                c = row.createCell(1);
                c.setCellValue(std_x);
                c = row.createCell(2);
                c.setCellValue(std_y);
                c = row.createCell(3);
                c.setCellValue(std_z);

                try {
                    FileOutputStream os = new FileOutputStream(file);
                    wb.write(os);
                    count++;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                index = 0;
                count = 0;
                start = false;
                count++;



            }
            else
                Confidence = 0;


        }
    };

    protected void onStart()
    {
        super.onStart();

        if (meter == null)
        {
            meter = new cs580.edu.binghamton.com.smartsilence.SoundMeter();
            meter.start();
        }
        else
            meter.start();
    }


    public void onResume() {
        super.onResume();

        sensorManager.registerListener(accelListener, sensor,
                400000);
    }

    public void onStop()
    {
        super.onStop();
        sensorManager.unregisterListener(accelListener);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        meter.stop();
        editor.apply();
    }



    public double ConvertAmplitudeToDecibel(double amplitude)
    {
        //double max = MediaRecorder.getAudioSourceMax();
        if(amplitude == 0)
            return  0;
        double dB = 20.0 * Math.log10(amplitude / 0.1); //32768
        return dB;
    }

    private class AsyncMeter extends AsyncTask<String, Void, String>
    {
        protected String doInBackground(String... params)
        {
            return Double.toString(meter.getAmplitude());
        }

        protected void onPostExecute(String result)
        {
            TextView tv = (TextView)findViewById(R.id.amplitudeTextView);
            TextView aTv = (TextView)findViewById(R.id.averageAmplitudeTextView);


            currentAmplitude = Double.parseDouble(result);

            if (amplitudeCount == 20) //Compute average for 20 counts
            {
                row_mic = sheet2.createRow(row_mic_count);
                row_mic_count++;
                c_mic = row_mic.createCell(0);
                c_mic.setCellValue(new SimpleDateFormat("HH:mm:ss").format(new Date()));
                c_mic = row_mic.createCell(1);
                averageAmplitude = totalAmplitude / amplitudeCount;
                c_mic.setCellValue(ConvertAmplitudeToDecibel(averageAmplitude));
                //ConvertAmplitudeToDecibel(averageAmplitude);
                aTv.setText("Average for 10 seconds in Decibels: \t\t" + String.format("%2.2f", ConvertAmplitudeToDecibel(averageAmplitude)) + " " + String.format("  count :   %d  ", amplitudeCount));
                try {
                    FileOutputStream os = new FileOutputStream(file);
                    wb.write(os);
                    count++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                averageAmplitude = 0; // reset values
                amplitudeCount = 0;
                totalAmplitude = 0;
            }

            if (true)
            {
                amplitudeCount++;
                totalAmplitude += Double.parseDouble(result);
                averageAmplitude = totalAmplitude / amplitudeCount;


                tv.setText("Current Amplitude:  \t\t" + String.format("%2.2f", Double.parseDouble(result)) + " " + String.format("  Decibels :   %2.2f dB ", ConvertAmplitudeToDecibel(Double.parseDouble(result))));
                //aTv.setText("Average Amplitude: \t\t" + String.format("%2.2f", averageAmplitude));
            }

            previousAmplitude = currentAmplitude;
        }


    }
}



