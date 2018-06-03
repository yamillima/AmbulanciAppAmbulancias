package com.ambulanciapp.ambulanciappambulancias;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    static public final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng Bangalore = new LatLng(12.978954, 77.589565);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Bangalore, 15));
        mMap.setMyLocationEnabled(true);

        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (enabled) {
            Toast.makeText(this, "Detectando ubicación", Toast.LENGTH_LONG).show();
        try {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                Task lastLocation = mFusedLocationProviderClient.getLastLocation();
                lastLocation.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Location mLastKnownLocation;
                            mLastKnownLocation = (Location)task.getResult();
                            LatLng mLastPosition = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLastPosition, 15));
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Activa el GPS", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
        }
    }

    private void llegada() {
        View cancelar = findViewById(R.id.cancelar);
        View confirmarLlegada = findViewById(R.id.confirmarLlegada);
        cancelar.setVisibility(View.INVISIBLE);
        confirmarLlegada.setVisibility(View.VISIBLE);
    }

    public void aceptarOnClick(View view) {
        View aceptar = findViewById(R.id.aceptar);
        View cancelar = findViewById(R.id.cancelar);
        aceptar.setVisibility(View.INVISIBLE);
        cancelar.setVisibility(View.VISIBLE);
        llegada();
    }

    public void confirmarLlegada(View view) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_layout, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setView(dialogLayout);
        alertBuilder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dispatchTakePictureIntent();
            }
        });
        alertBuilder.setCancelable(false);
        alertBuilder.show();
        View confirmarLlegada = findViewById(R.id.confirmarLlegada);
        View cancelar = findViewById(R.id.cancelar);
        confirmarLlegada.setVisibility(View.INVISIBLE);
        cancelar.setVisibility(View.VISIBLE);
    }

    static final int REQUEST_IMAGE_CAPTURE =1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            Toast.makeText(this, "Toma la foto y toca el signo de verificación para continuar", Toast.LENGTH_LONG).show();
        }
    }

    private class downloadJSON extends AsyncTask<LatLng, Integer, String> {

        ProgressBar progressBar = findViewById(R.id.progressBar);

        @Override
        protected String doInBackground(LatLng... params) {

            String data = "";
            HttpsURLConnection connection;
            InputStream inputStream;
            LatLng myLocation = params[0];
            LatLng NeivaHospital = new LatLng(2.932305, -75.280828);
            String origin = String.valueOf(myLocation.latitude)+","+String.valueOf(myLocation.longitude);
            String destination = String.valueOf(NeivaHospital.latitude)+","+String.valueOf(NeivaHospital.longitude);
            String stringURL = "https://maps.googleapis.com/maps/api/directions/json?origin="+origin+"&destination="+destination+"&key=AIzaSyAr-2B-oysrClb_YMMjgRIyvv51Km3WHug";

            try {
                    URL myUrl = new URL(stringURL);
                    connection = (HttpsURLConnection) myUrl.openConnection();
                    connection.connect();
                    inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuffer = new StringBuilder();
                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line);
                        int currentProgress = stringBuffer.length();
                        publishProgress(currentProgress);
                        data = stringBuffer.toString();
                    }

                    bufferedReader.close();
                    inputStream.close();
                    connection.disconnect();

            } catch(Exception e) {
                Log.d("Error",e.toString());
            }

            return data;
        }

        @Override
        protected void onProgressUpdate(Integer... progreso) {
            TextView contador = findViewById(R.id.progreso);
            contador.setText(String.valueOf(progreso[0]));
            progressBar.setProgress(progreso[0]);
        }

        @Override
        protected void onPreExecute() {
            progressBar.setMax(15000);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String data) {
            String polyline ="polyline";
            try {
                JSONObject json = new JSONObject(data);
                JSONArray routes = json.getJSONArray("routes");
                for (int i = 0; i < routes.length(); i++) {
                    JSONObject overview_polyline = routes.getJSONObject(i).getJSONObject("overview_polyline");
                    polyline = overview_polyline.getString("points");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            List<LatLng> decodedPolyLine = PolyUtil.decode(polyline);
            int myBlue = Color.rgb(71, 173, 241);
            //progressBar.setVisibility(View.INVISIBLE);
            mMap.addPolyline(new PolylineOptions().addAll(decodedPolyLine).color(myBlue).geodesic(true));
            View ingresarSOAT = findViewById(R.id.ingresarSOAT);
            progressBar.setVisibility(View.INVISIBLE);
            ingresarSOAT.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Enviando foto", Toast.LENGTH_LONG).show();
            LatLng myLocation = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
            LatLng NeivaHospital = new LatLng(2.932305, -75.280828);
            mMap.addMarker(new MarkerOptions().position(NeivaHospital).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, (float)13.5));
            new downloadJSON().execute(myLocation);
        } else {
            dispatchTakePictureIntent();
        }
    }

    public void ingresarSOAT(View view) {
        LayoutInflater inflater = getLayoutInflater();
        View soatLayout = inflater.inflate(R.layout.soat_layout, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setView(soatLayout);
        alertBuilder.setPositiveButton("Enviar", null);
        alertBuilder.show();

    }

}
