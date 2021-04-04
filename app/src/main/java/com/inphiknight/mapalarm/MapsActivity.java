package com.inphiknight.mapalarm;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Calendar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FusedLocationProviderClient client;
    private LatLng currentLoc;
    private Circle mapCircle;
    private Float triggerRadius;
    private Marker redMarker;
    private EditText refLocTime;


    private EditText triggerRadiusText;

    //initialization of saved last trigger location
    private SharedPreferences.Editor edit;
    private SharedPreferences preferenceSettings;
    private LatLng triggerLoc;

    Dialog myDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        myDialog = new Dialog(this);
        preferenceSettings = getSharedPreferences("Test", Context.MODE_PRIVATE);
        edit = preferenceSettings.edit();
        triggerLoc = new LatLng(preferenceSettings.getFloat("trigLat", (float) 26.585), preferenceSettings.getFloat("trigLong", (float) 93.168));

        edit.putFloat("refreshRateLoc", (float) 5000);
        edit.apply();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.spinner1);
        //create a list of items for the spinner.
        String[] items = new String[]{"Metres", "Kms", "Miles"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
        //selection
        if(preferenceSettings.getInt("measureSys", 0) == 1){
            dropdown.setSelection(1);
        }else{
            if(preferenceSettings.getInt("measureSys", 0) == 2){
                dropdown.setSelection(2);
            }
        }

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                edit.putInt("measureSys", position);
                edit.apply();

                //draw circle
                if(mapCircle!=null){
                    mapCircle.remove();
                }

                if(position == 1){
                    mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1000).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                }else{
                    if(position == 2){
                        mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1609.34).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                    }
                    else{
                        mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //text space
        triggerRadiusText = (EditText) findViewById(R.id.triggerRadius5);
        triggerRadiusText.setText(String.valueOf(preferenceSettings.getFloat("triggerRadius", 0)));
        triggerRadiusText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    triggerRadius = (float) 0;
                }else{
                    triggerRadius = Float.parseFloat(s.toString());
                }
                edit.putFloat("triggerRadius", triggerRadius);
                edit.apply();
                //Log.v("trigger Radius", String.valueOf(preferenceSettings.getFloat("triggerRadius", 0)));

                //draw circle
                if(mapCircle!=null){
                    mapCircle.remove();
                }

                if(preferenceSettings.getInt("measureSys", 0) == 1){
                    mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1000).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                }else{
                    if(preferenceSettings.getInt("measureSys", 0) == 2){
                        mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1609.34).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                    }
                    else{
                        mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                    }
                }

            }
        });


        //switch
        if(isLocationServiceRunning() || (LocationService.mp != null ? LocationService.mp.isPlaying() : false)){
            Switch mySwitch = (Switch) findViewById(R.id.switch2);
            mySwitch.setChecked(true);
        }

        Switch mySwitch = (Switch) findViewById(R.id.switch2);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked == true){
                    //Log.v("Switch State=", ""+isChecked);
                    if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }else{
                        startLocationService();
                        //Log.v("Trigger Location=", " "+ triggerLoc.latitude + " " + triggerLoc.longitude);
                    }
                }
                if(isChecked == false){
                    //Log.v("Switch State=", ""+isChecked);
                    stopLocationService();
                    if(LocationService.mp != null ? !LocationService.mp.isPlaying() : false){
                        LocationService.mp.reset();
                    }
                    if(LocationService.mp != null ? LocationService.mp.isPlaying() : false){
                        LocationService.mp.stop();
                        LocationService.mp.reset();

                    }
                }
            }
        });



    }

    //initial marker drawn
    private void getCurrentLocation() {
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //last location not older than 30 secs
                if(location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 30 * 1000){
                    currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLoc).title("I'm here").draggable(false).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 10));
                }
                else{
                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(currentLoc).title("I'm here").draggable(false).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 10));
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
                    };

                    // Now first make a criteria with your requirements
                    // this is done to save the battery life of the device
                    // there are various other other criteria you can search for..
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    criteria.setPowerRequirement(Criteria.POWER_LOW);
                    criteria.setAltitudeRequired(false);
                    criteria.setBearingRequired(false);
                    criteria.setSpeedRequired(false);
                    criteria.setCostAllowed(true);
                    criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
                    criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

                    // Now create a location manager
                    final LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

                    // This is the Best And IMPORTANT part
                    final Looper looper = null;

                    locationManager.requestSingleUpdate(criteria, locationListener, looper);
                }

            }
        });
    }

    //Location background services
    private boolean isLocationServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if(activityManager != null){
            for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)){
                if(LocationService.class.getName().equals(service.service.getClassName())){
                    if(service.foreground){
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent intent =  new Intent(getApplicationContext(), LocationService.class);
            intent.setAction("startLocationService");
            startService(intent);
            Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();
            redMarker.remove();
            redMarker = mMap.addMarker(new MarkerOptions().position(triggerLoc).title("Trigger location").draggable(false));
        }
    }

    private void stopLocationService(){
        if(isLocationServiceRunning()){
            Intent intent =  new Intent(getApplicationContext(), LocationService.class);
            intent.setAction("stopLocationService");
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
            redMarker.remove();
            redMarker = mMap.addMarker(new MarkerOptions().position(triggerLoc).title("Trigger location").draggable(true));
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //draw circle
        triggerRadius = new Float(preferenceSettings.getFloat("triggerRadius", 0));
        if(mapCircle!=null){
            mapCircle.remove();
        }
        if(preferenceSettings.getInt("measureSys", 0) == 1){
            mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1000).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
        }else{
            if(preferenceSettings.getInt("measureSys", 0) == 2){
                mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1609.34).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
            }
            else{
                mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
            }
        }

        //fused location
        client = LocationServices.getFusedLocationProviderClient(this);

        //permissions
        if(ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            //if given
            getCurrentLocation();
        }
        else{
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        // Add a marker in Sydney and move the camera
        redMarker = mMap.addMarker(new MarkerOptions().position(triggerLoc).title("Trigger location").draggable(isLocationServiceRunning() ? false : true));

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {
                if(mapCircle!=null){
                    mapCircle.remove();
                }
                //Toast.makeText(MapsActivity.this, "Dragging Start", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                triggerLoc = marker.getPosition();

                edit.putFloat("trigLat", (float) triggerLoc.latitude);
                edit.putFloat("trigLong", (float) triggerLoc.longitude);
                edit.apply();

                Toast.makeText(
                        MapsActivity.this,
                        "Lat " + triggerLoc.latitude + " "
                                + "Long " + triggerLoc.longitude,
                        Toast.LENGTH_LONG).show();
                //draw circle
                if(preferenceSettings.getInt("measureSys", 0) == 1){
                    mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1000).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                }else{
                    if(preferenceSettings.getInt("measureSys", 0) == 2){
                        mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius*1609.34).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                    }
                    else{
                        mapCircle = mMap.addCircle(new CircleOptions().center(triggerLoc).radius(triggerRadius).strokeColor(Color.RED).fillColor(0x220000FF).strokeWidth(5));
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) { }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 44){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1 && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startLocationService();
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void ShowPopUp(View v){
        TextView txtclose;
        myDialog.setContentView(R.layout.settings_popup);
        txtclose = (TextView) myDialog.findViewById(R.id.popupClose);

        //text space
        refLocTime = (EditText) myDialog.findViewById(R.id.refTimeLoc);
        refLocTime.setText(String.valueOf(preferenceSettings.getFloat("refreshRateLoc", (float) 5000)/1000));
        refLocTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    edit.putFloat("refreshRateLoc", (float) 5000);
                }else{
                    edit.putFloat("refreshRateLoc", Float.parseFloat(s.toString())*1000);
                }
                edit.apply();
            }
        });

        txtclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.setCanceledOnTouchOutside(false);
        myDialog.setCancelable(false);
        myDialog.show();


    }

}