package com.example.CultureLoisirs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Vector;

public class MainActivity extends Activity{

    private GoogleMap map;
    private ListView list;
    private EditText villeTxt;
    private Spinner spin;
    private boolean isList = false;
    private Bitmap img;

    private Vector<Marker> markList = new Vector<Marker>();
    private Vector<Double> latList = new Vector<Double>();
    private Vector<Double> lngList = new Vector<Double>();
    private Vector<String> noms = new Vector<String>();
    private Vector<String> adresses = new Vector<String>();
    private Vector<String> photos = new Vector<String>();
    private Double latCity;
    private Double lngCity;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                         @Override
                                         public boolean onMarkerClick(Marker marker) {
                                             popup(marker);
                                             return false;
                                         }
                                     }
            );

        list = ((ListView) findViewById(R.id.listView));
        ArrayAdapter<String> adapt = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new ArrayList<String>());
        list.setVisibility(View.INVISIBLE);

        villeTxt = ((EditText) findViewById(R.id.editText2));

        spin = ((Spinner) findViewById(R.id.spinner));

        Switch onOffSwitch = (Switch) findViewById(R.id.togglebutton);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switchView();
            }

        });

        LocationListener onLocationChange=new LocationListener() {
            public void onLocationChanged(Location location) {
                CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()),12.0f);
                map.moveCamera(camUpdate);
            }

            public void onProviderDisabled(String provider) {
                // required for interface, not used
            }

            public void onProviderEnabled(String provider) {
                // required for interface, not used
            }

            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                // required for interface, not used
            }
        };

       LocationManager mgr=(LocationManager)getSystemService(LOCATION_SERVICE);
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,3600000, 1000,onLocationChange);
        Location loc = mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(loc!=null)
        {
            Log.d("loclat","" + loc.getLatitude());
            CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()),12.0f);
            map.moveCamera(camUpdate);
        }

    }


    public void search(View v) {
            /*findViewById(R.id.maplayout).setVisibility(View.INVISIBLE);
        list.setVisibility(View.VISIBLE);*/
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);


        //On efface les résultats de la recherche précédente
        noms.clear();
        adresses.clear();
        latList.clear();
        lngList.clear();
        markList.clear();
        photos.clear();

        String ville = villeTxt.getText().toString();


        //Utilisation de l'API Google Geocode pour obtenir les coordonnées de la ville recherchée
        String uri = "https://maps.google.com/maps/api/geocode/json?address=" +
                ville.toLowerCase().trim() + "&sensor=false";
        Log.d("uri",uri);

        //Execution du thread de Geocoding (recherche des coordonnées de la ville entrée)
        new GoogleGeocodeData().execute(uri);

        String[] a = new String[noms.size()];
        for(int i = 0; i < noms.size(); i++)
            a[i] = noms.elementAt(i);



        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, a);
        list.setAdapter(adapter);
        //list.setVisibility(View.VISIBLE);


    }

    public void popup(Marker mark) {

        //Recherche de la photo associée au lieu pointé par le marqueur
        ImageView image = null;
        String photo = null;
        if(photos.size() != 0)
        {
            photo = photos.elementAt(markList.indexOf(mark));
            String uri = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=250&maxheight=250&photoreference=" + photo + "&key=AIzaSyC0lvf2ls1r52QONC137dzVEYF11fpAH7Q";
            new GooglePhotos().execute(uri);

            //Création de l'objet image
            image = new ImageView(this);
            image.setImageBitmap(img);
        }




        //Dialog contenant les infos du lieu
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(mark.getTitle())
                .setMessage(mark.getSnippet())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert).create();
                if(photo != null)
                {
                    alert.setView(image);
                }
                alert.show();

    }

    public void switchView() {
        if (isList == false) {
            isList = true;
            View lay = ((View) findViewById(R.id.maplayout));
            lay.setVisibility(View.INVISIBLE);
            View lay2 = ((View) findViewById(R.id.listView));
            lay2.setVisibility(View.VISIBLE);
        } else {
            isList = false;
            View lay = ((View) findViewById(R.id.maplayout));
            lay.setVisibility(View.VISIBLE);
            View lay2 = ((View) findViewById(R.id.listView));
            lay2.setVisibility(View.INVISIBLE);
        }
    }



    /*@Override
    public boolean onMarkerClick(Marker marker) {
        //popup(marker);
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(marker.getTitle())
                .setMessage(marker.getSnippet())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        dialog.dismiss();
                    }
                })
                /*.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        return true;
    }*/


    private class GoogleGeocodeData  extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String[] urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return getCoords(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {


            //https://maps.googleapis.com/maps/api/place/search/json?location=40,40&radius=20000&types=bar&key=AIzaSyDkKKV4ABzNVf33GfCzxgLAYUvjTZseNHQ
            //Utilisation de l'API Google Places pour rechercher les lieux du type choisi
            String type =  getResources().getStringArray(R.array.type_values)[spin.getSelectedItemPosition()];
            String uriPlaces = "https://maps.googleapis.com/maps/api/place/search/json?location=" + latCity + "," + lngCity +
                    "&radius=5000&types=" + type + "&key=AIzaSyDkKKV4ABzNVf33GfCzxgLAYUvjTZseNHQ";

            //Execution du thread de recherche des lieux
            new GooglePlacesData().execute(uriPlaces);
        }

        public String getCoords(String uri) throws IOException
        {
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("accept", "application/json");
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            StringBuilder stringBuilder = new StringBuilder();
            String res = null;
            try{
                response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                InputStream stream = entity.getContent();
                int b;
                while ((b = stream.read()) != -1) {
                    stringBuilder.append((char) b);
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(stringBuilder.toString());

                String lng = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getString("lng");

                String lat = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getString("lat");
                res = lat + ";" + lng;
                latCity = Double.valueOf(lat);
                lngCity = Double.valueOf(lng);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            client.getConnectionManager().shutdown();
            return res;
        }
    }


    private class GooglePlacesData  extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String[] urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                Log.d("test","test");
                return getMarkers(urls[0]);
            } catch (IOException e) {
                //return "Unable to retrieve web page. URL may be invalid.";
                return new Boolean("false");
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("size", " : " + latList.size());
            map.clear();
            for(int i = 0; i < latList.size(); i++)
            {
                Marker mark = map.addMarker(new MarkerOptions()
                        .position(new LatLng(latList.elementAt(i),lngList.elementAt(i)))
                        .title(Normalizer.normalize(noms.elementAt(i), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""))
                        .snippet(Normalizer.normalize(adresses.elementAt(i), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")));
                markList.add(mark);
            }

            Log.d("lat",latCity.toString());
            Log.d("lng",lngCity.toString());
            CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(latCity, lngCity),12.0f);
            map.moveCamera(camUpdate);
        }

        public Boolean getMarkers(String uri) throws IOException
        {
            Boolean res = new Boolean("false");
            HttpGet httpGetPlace = new HttpGet(uri);
            httpGetPlace.addHeader("accept", "application/json");
            HttpClient clientPlace = new DefaultHttpClient();
            HttpResponse responsePlace;
            StringBuilder stringBuilderPlace = new StringBuilder();
            try{
                responsePlace = clientPlace.execute(httpGetPlace);
                HttpEntity entity = responsePlace.getEntity();
                InputStream stream = entity.getContent();
                int b;
                while ((b = stream.read()) != -1) {
                    stringBuilderPlace.append((char) b);
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }


            try {
                JSONObject jsonObjectPlace;
                jsonObjectPlace = new JSONObject(stringBuilderPlace.toString());
                JSONArray results = jsonObjectPlace.getJSONArray("results");
                for(int i = 0; i < results.length(); i++)
                {
                    JSONObject result = results.getJSONObject(i);
                    JSONObject geometry = result.getJSONObject("geometry");
                    JSONObject location = geometry.getJSONObject("location");
                    latList.add(location.getDouble("lat"));
                    lngList.add(location.getDouble("lng"));
                    //JSONObject nameJSON = result.getJSONObject("name");
                    noms.add(result.getString("name"));
                    adresses.add(result.getString("vicinity"));
                    JSONArray photoObj = result.getJSONArray("photos");
                    photos.add(photoObj.getJSONObject(0).getString("photo_reference"));

                }
                res = true;
            /*lat = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat");*/
                clientPlace.getConnectionManager().shutdown();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            return res;
        }


    }

    private class GooglePhotos  extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String[] urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return getPhoto(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

        }

        public String getPhoto(String uri) throws IOException
        {
            HttpGet httpGet = new HttpGet(uri);
            //httpGet.addHeader("accept", "application/json");
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            StringBuilder stringBuilder = new StringBuilder();
            String res = null;
            try{
                response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                InputStream stream = entity.getContent();
                img = BitmapFactory.decodeStream(stream);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            client.getConnectionManager().shutdown();
            return res;
        }
    }

}
