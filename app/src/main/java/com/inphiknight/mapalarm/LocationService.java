package com.inphiknight.mapalarm;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.CircleOptions;

import java.io.IOException;

public class LocationService extends Service{

    private float distance;
    //private int refreshTime = 5000;
    private Location destinationLoc1 = new Location("");
    private Location currentLoc1 = new Location("");
    private LocationRequest locationRequest;
    public static MediaPlayer mp;

    private SharedPreferences.Editor edit;
    private SharedPreferences preferenceSettings;
    private int measureSystem;

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


                if(measureSystem == 1){
                    distance = distance/1000;
                }else{
                    if(measureSystem == 2) {
                        distance = (float) (distance/1609.34);
                    }
                }
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
                    if(mp == null)
                    {
                        mp = new MediaPlayer();
                    }
                    if(!mp.isPlaying()){
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
                        //Log.v("Play sounds", String.valueOf(123));
                    }
                }




                //distance/speed in m/s
                //((distance/20) * 1000)/2 =
                //refreshTime = (int) (distance*25);


                //Log.v("Destination distance", String.valueOf(distance));

            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    private void startLocationService(){
        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);

        builder.setSmallIcon(R.mipmap.mapalarm_icon);
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
        preferenceSettings = getSharedPreferences("Test", Context.MODE_PRIVATE);
        edit = preferenceSettings.edit();
        measureSystem = preferenceSettings.getInt("measureSys", 0);
        locationRequest.setInterval((long) preferenceSettings.getFloat("refreshRateLoc", 5000));
        locationRequest.setFastestInterval((long) (preferenceSettings.getFloat("refreshRateLoc", 5000) - 2000));
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
