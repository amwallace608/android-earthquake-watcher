package com.amwallace.earthquakewatcher.Activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.amwallace.earthquakewatcher.Model.Earthquake;
import com.amwallace.earthquakewatcher.R;
import com.amwallace.earthquakewatcher.UI.CustomInfoWindow;
import com.amwallace.earthquakewatcher.Util.Constants;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue queue;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;
    private BitmapDescriptor[] iconColors;
    private Button showListBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        showListBtn = (Button) findViewById(R.id.showListBtn);
        //show list button click listener
        showListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to new earthquake list activity
                startActivity(new Intent(MapsActivity.this, QuakeListActivity.class));
            }
        });

        //bitmap descriptor array for icon colors
        iconColors = new BitmapDescriptor[]{
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
        };


        //instantiate req queue
        queue = Volley.newRequestQueue(this);
        //get earthquake data function
        getEarthquakes();
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
        //set custom window adapter
        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        //register on click listeners
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

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
        //check sdk version
        if (Build.VERSION.SDK_INT < 26) {
            //check permission, ask for permission if not already granted
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            } else {
                //have permission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 0, locationListener);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //check if permission granted
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            //granted
            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                //request location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 0, locationListener);
            }
            //save last known location of GPS
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    private void getEarthquakes() {
        final Earthquake earthquake = new Earthquake();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray features = response.getJSONArray("features");
                    //loop through earthquakes/features (up to limit set in constants)
                    for(int i = 0; i < Constants.LIMIT; i++){
                        //get properties of earthquake i in array
                        JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
                        //get coordinates of earthquake i
                        JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                        //get coordinates array
                        JSONArray coordinates = geometry.getJSONArray("coordinates");
                        //longitude - first index of coordinates array
                        double lon = coordinates.getDouble(0);
                        //latitude - second index of coordinates array
                        double lat = coordinates.getDouble(1);
                        //set earthquake data
                        earthquake.setPlace(properties.getString("place"));
                        earthquake.setType(properties.getString("type"));
                        earthquake.setLat(lat);
                        earthquake.setLon(lon);
                        earthquake.setMagnitude(properties.getDouble("mag"));
                        earthquake.setDetailLink(properties.getString("detail"));
                        earthquake.setTime(properties.getLong("time"));
                        //get formatted time
                        DateFormat dateFormat = DateFormat.getDateInstance();
                        String formattedDate = dateFormat.format(
                                new Date(Long.valueOf(properties.getLong("time")))
                                        .getTime());

                        MarkerOptions markerOptions = new MarkerOptions();
                        //assign marker color based on magnitude
                        markerOptions.icon(iconColors[Constants.colorScale(earthquake.getMagnitude())]);
                        //assign circle marker around magnitude greater than 5
                        if(earthquake.getMagnitude() >= 5.0){
                            CircleOptions circleOptions = new CircleOptions();
                            circleOptions.center(new LatLng(lat,lon));
                            circleOptions.radius(35000);
                            circleOptions.strokeWidth(2.8f);
                            circleOptions.fillColor(Color.RED);
                            mMap.addCircle(circleOptions);
                        }

                        //fill in marker info
                        markerOptions.title(earthquake.getPlace());
                        markerOptions.position(new LatLng(lat,lon));
                        markerOptions.snippet("Magnitude: " + earthquake.getMagnitude() + "\n"+
                                "Date: " + formattedDate);

                        Marker marker = mMap.addMarker(markerOptions);
                        marker.setTag(earthquake.getDetailLink());
                        mMap.animateCamera(CameraUpdateFactory
                                .newLatLngZoom(new LatLng(lat,lon),1));

                    }

                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {//in case of error
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        //add to req queue
        queue.add(jsonObjectRequest);
    }

    //get earthquake details from "details" link
    private void getEarthquakeDetails(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String detailsUrl = "";
                try {
                    //get geoserve Json Array
                    JSONObject properties = response.getJSONObject("properties");
                    JSONObject products = properties.getJSONObject("products");
                    JSONArray geoserve = products.getJSONArray("geoserve");

                    //loop through geoserve array
                    for (int i = 0; i < geoserve.length(); i++){
                        JSONObject geoserveObj = geoserve.getJSONObject(i);
                        JSONObject contentObj = geoserveObj.getJSONObject("contents");
                        JSONObject geoJsonObj = contentObj.getJSONObject("geoserve.json");
                        detailsUrl = geoJsonObj.getString("url");
                    }
                    getMoreDetails(detailsUrl);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        //add to queue
        queue.add(jsonObjectRequest);
    }

    public void getMoreDetails(String url){
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //create new alert dialog
                dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                View view = getLayoutInflater().inflate(R.layout.details_popup, null);
                //setup widgets
                Button dismissBtn = (Button) view.findViewById(R.id.dismissPopBtm);
                Button dismissBtnTop = (Button) view.findViewById(R.id.dismissPopTop);
                TextView popList = (TextView) view.findViewById(R.id.popupList);
                WebView popupWebView = (WebView) view.findViewById(R.id.popupWebView);

                //string builder for cities
                StringBuilder stringBuilder = new StringBuilder();

                //get JSON array of nearby cities
                try {
                    //get tectonicSummary text field if it exists for the earthquake
                    if(response.has("tectonicSummary")
                            && response.getString("tectonicSummary") != null){
                        //get tectonicSummary object
                        JSONObject tectonicSummary = response.getJSONObject("tectonicSummary");
                        //check that text field is included and not null
                        if(tectonicSummary.has("text")
                                && tectonicSummary.getString("text") != null){
                            //get text html string
                            String text = tectonicSummary.getString("text");
                            //load html into popup web view
                            popupWebView.loadDataWithBaseURL(null, text,
                                    "text/html","UTF-8", null);

                        }
                    }

                    JSONArray cities = response.getJSONArray("cities");
                    //loop through each city
                    for (int i = 0; i < cities.length(); i++){
                        JSONObject city = cities.getJSONObject(i);
                        //add city info to stringBuilder string
                        stringBuilder.append("City: " + city.getString("name") + "\n" +
                                "Distance: " + city.getString("distance") + " "
                                + city.getString("direction") + "\n" +
                                "Population: " + city.getString("population") + "\n\n");
                    }
                    //set popList textview to show string of cities
                    popList.setText(stringBuilder);

                    //dismiss button click listeners
                    dismissBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialog.dismiss();
                        }
                    });
                    dismissBtnTop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialog.dismiss();
                        }
                    });

                    dialogBuilder.setView(view);
                    alertDialog = dialogBuilder.create();
                    alertDialog.show();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        //add to queue
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        getEarthquakeDetails(marker.getTag().toString());
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }


}
