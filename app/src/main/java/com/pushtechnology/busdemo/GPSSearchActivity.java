package com.pushtechnology.busdemo;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;


public class GPSSearchActivity extends AppCompatActivity {

    private String longlat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpssearch);

        new Thread() {
            @Override
            public void run() {
                new BusStopAdmin();
                while(true);
            }
        }.start();

        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new PhoneLocationListener());
        }

    }

    public void getGPS(View view) {
        final Intent i = new Intent(GPSSearchActivity.this, BusStopsActivity.class);
        i.putExtra("location", longlat);
        startActivity(i);
    }

    private final class PhoneLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            longlat = Double.toString(location.getLongitude()) + ',' + Double.toString(location.getLatitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            if (i == LocationProvider.OUT_OF_SERVICE) {
                Log.v("GPS", "Provider status: OUT_OF_SERVICE");
            }
            else if (i == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                Log.v("GPS", "Provider status: TEMPORARILY_UNAVAILABLE");
            }
            else {
                Log.v("GPS", "Provider status: AVAILABLE");
            }

        }

        @Override
        public void onProviderEnabled(String s) {
            Log.v("GPS", "Provider enabled: " + s);
        }

        @Override
        public void onProviderDisabled(String s) {
            Log.v("GPS", "Provider disabled: " + s);
        }
    }
}


