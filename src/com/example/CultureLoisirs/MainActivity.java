package com.example.CultureLoisirs;

import android.app.Activity;
import android.os.Bundle;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

public class MainActivity extends Activity {

    private GoogleMap map;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
    }
}
