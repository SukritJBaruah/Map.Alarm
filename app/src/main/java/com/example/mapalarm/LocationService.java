package com.example.mapalarm;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service{

    private float distance;
    private int refreshTime = 5000;
    private Location destinationLoc1 = new Location("");
    private Location currentLoc1 = new Location("");
    private LocationRequest locationRequest;
    public static MediaPlayer mp;

    private SharedPreferences.Editor edit;
    private SharedPreferences preferenceSettings;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if(locationResult != null && locationResult.getLastLocation() != null)
            {
                //double latitude = locationResult.getLastLocation().getLatitude();
                //double longitude = locationResult.getLastLocation().getLongitude();
                //debug
                //Log.v("Location Update", latitude + ", " + longitude);


                //Main algorithm
                preferenceSettings = getSharedPreferences("Test", Context.MODE_PRIVATE);
                edit = preferenceSettings.edit();

                destinationLoc1.setLatitude(preferenceSettings.getFloat("trigLat", (float) 26.585));
                destinationLoc1.setLongitude(preferenceSettings.getFloat("trigLong", (float) 93.168));

                currentLoc1.setLatitude(locationResult.getLastLocation().getLatitude());
                currentLoc1.setLongitude(locationResult.getLastLocation().getLongitude());

                distance = destinationLoc1.distanceTo(currentLoc1);


                //alarm sound
                if(distance <= preferenceSettings.getFloat("triggerRadius", 0)){
                    Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    if (alarmUri == null) {
                        alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    }

                    if (alarmUri == null) {
                        Log.e("ringAlarm" , "alarmUri null. Unable to get default sound URI");
                        return;
                    }

                    // This is what sets the media type as alarm
                    // Thus, the sound will be influenced by alarm volume
                    mp = new MediaPlayer();
                    mp.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM).build());

                    try {
                        mp.setDataSource(getApplicationContext(), alarmUri);
                        mp.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // To continuously loop the alarm sound
                    mp.setLooping(true);
                    mp.start();
                    stopLocationService();
                }




                //distance/speed in m/s
                //((distance/20) * 1000)/2 =
                refreshTime = (int) (distance*25);

                if(mp != null ? !mp.isPlaying() : false){
                    stploctemp();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            strtloctemp();
                        }
                    }, refreshTime);
                }


                Log.v("Destination distance", String.valueOf(distance) + " Time/2: " + refreshTime/1000);

            }
        }
    };

    //Maybe a bad way to make update location varying, check this later
    private void stploctemp(){
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
    }
    private void strtloctemp(){
        locationRequest.setInterval(refreshTime);
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    private void startLocationService(){
        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentText("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Location Alarm is Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(notificationManager != null && notificationManager.getNotificationChannel(channelId) == null){
                NotificationChannel notificationChannel = new NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);

            }
        }

        locationRequest = new LocationRequest();
        locationRequest.setInterval(refreshTime);
        locationRequest.setFastestInterval(refreshTime - 2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(175, builder.build());

    }

    private void stopLocationService(){
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                if(action.equals("startLocationService")){
                    startLocationService();
                } else if(action.equals("stopLocationService")){
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
