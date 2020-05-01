package com.amwallace.earthquakewatcher.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.amwallace.earthquakewatcher.Model.Earthquake;
import com.amwallace.earthquakewatcher.R;
import com.amwallace.earthquakewatcher.Util.Constants;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuakeListActivity extends AppCompatActivity {

    private ArrayList<String> arrayList;
    private ListView listView;
    private RequestQueue queue;
    private ArrayAdapter arrayAdapter;

    private List<Earthquake> earthquakeList;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quake_list);
        //instantiate earthquake list and listview
        earthquakeList = new ArrayList<>();
        listView = (ListView) findViewById(R.id.quakeListView);
        //get new request queue
        queue = Volley.newRequestQueue(this);

        arrayList = new ArrayList<>();

        getAllQuakes();
    }

    private void getAllQuakes(){
        //json request to usgs to get all earthquake data within the last week
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    JSONArray features = response.getJSONArray("features");
                    //clear lists
                    arrayList.clear();
                    earthquakeList.clear();
                    //loop through all earthquakes/features from the last week
                    for(int i = 0; i < features.length(); i++){
                        Earthquake earthquake = new Earthquake();
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

                        //add place of quake to arraylist
                        arrayList.add(earthquake.getPlace());
                        earthquakeList.add(earthquake);
                    }
                    //setup array adapter for array list, simple list item for each earthquake place
                    arrayAdapter = new ArrayAdapter(QuakeListActivity.this,
                            android.R.layout.simple_list_item_1, android.R.id.text1, arrayList);
                    listView.setAdapter(arrayAdapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            //get earthquake detail and show dialog for clicked earthquake
                            getEarthquakeDetails(earthquakeList.get(position).getDetailLink());
                        }
                    });

                    arrayAdapter.notifyDataSetChanged();


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
                dialogBuilder = new AlertDialog.Builder(QuakeListActivity.this);
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


}
