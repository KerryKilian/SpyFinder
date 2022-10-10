package com.example.mapbox1;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.annotation.NonNull;
// Classes needed to initialize the map
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
// Classes needed to handle location permissions
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;


import java.util.ArrayList;
import java.util.List;
// Classes needed to add the location engine
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import java.lang.ref.WeakReference;
// Classes needed to add the location component
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;



/**
 * Use the Mapbox Core Library to listen to device location updates
 */
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {

    Button button;
    Button noCameras;
    // Variables needed to initialize a map
    private MapboxMap mapboxMap;
    private MapView mapView;
    // Variables needed to handle location permissions
    private PermissionsManager permissionsManager;
    // Variables needed to add the location engine
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    // Variables needed to listen to location updates
    private MainActivityLocationCallback callback = new MainActivityLocationCallback(this);
    double[] onlyNearGeofencesLatArray;
    double[] onlyNearGeofencesLonArray;
    double[] dangerousAreaLatArray;
    double[] dangerousAreaLonArray;
    private AlertDialog dialog;
    boolean allCamerasBoolean;
    Switch allCamerasSwitch;
    TextView whatPointsShown;
    String bundesland;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beenden();
            }

        });

        noCameras = (Button) findViewById(R.id.noCameras);
        noCameras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Dir werden keine Standorte angezeigt?")
                        .setMessage("Überprüfe, ob du dich in deinem ausgewählten Bundesland befindest.\nFalls ja, geh zurück und starte die Karte nach einigen Sekunden neu.\nFalls dir immer noch keine Kameras angezeigt werden, sind in deiner Nähe keine Kameras gelistet.")
                        .setPositiveButton("Alles klar", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                dialog.cancel();

                            }
                        })
                        .show();
            }
        });

        whatPointsShown = (TextView) findViewById(R.id.whatPointsShown);

        allCamerasSwitch = (Switch) findViewById(R.id.allCamerasSwitch);




        // Aus HauptActivity werden double Listen genommen, um damit in der Karte die Standorte anzuzeigen
        Bundle bundle = getIntent().getExtras();
        onlyNearGeofencesLatArray = bundle.getDoubleArray("onlyNearGeofencesLatArray");
        onlyNearGeofencesLonArray = bundle.getDoubleArray("onlyNearGeofencesLonArray");
        dangerousAreaLatArray = bundle.getDoubleArray("dangerousAreaLatArray");
        dangerousAreaLonArray = bundle.getDoubleArray("dangerousAreaLonArray");
        bundesland = bundle.getString("item");


    }

    public void beenden(){
        this.finish();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(Style.MAPBOX_STREETS,
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);

                    }
                });


        if (onlyNearGeofencesLatArray != null){ // Marker sollen erst eingezeichnet werden, wenn in HauptActivity Punkte erstellt wurden, ansonsten werden keine Punkte eingezeichnet
            MarkerOptions marker = new MarkerOptions(); // neuer Marker, kompliziert, da Google und Mapbox beide eine LatLng klasse beistzen, deswegen über double Listen
            for (int x = 0; x < onlyNearGeofencesLatArray.length; x++){
                marker.position(new LatLng(onlyNearGeofencesLatArray[x], onlyNearGeofencesLonArray[x]));
                mapboxMap.addMarker(marker);
            }
            whatPointsShown.setText("In dieser Ansicht werden alle in deiner Nähe befindlichen Kameras angezeigt");
        }



        allCamerasSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mapboxMap.clear();
                    if (dangerousAreaLatArray != null){ // Marker sollen erst eingezeichnet werden, wenn in HauptActivity Punkte erstellt wurden, ansonsten werden keine Punkte eingezeichnet
                        MarkerOptions marker = new MarkerOptions(); // neuer Marker, kompliziert, da Google und Mapbox beide eine LatLng klasse beistzen, deswegen über double Listen
                        for (int x = 0; x < dangerousAreaLatArray.length; x++){
                            marker.position(new LatLng(dangerousAreaLatArray[x], dangerousAreaLonArray[x]));
                            mapboxMap.addMarker(marker);
                            whatPointsShown.setText("In dieser Ansicht werden alle Kameras aus " + bundesland + " angezeigt");
                        }
                    }
                } else {
                    mapboxMap.clear();
                    // Marker an der Stelle setzen, wo Überwachungskameras in der Nähe sind
                    if (onlyNearGeofencesLatArray != null){ // Marker sollen erst eingezeichnet werden, wenn in HauptActivity Punkte erstellt wurden, ansonsten werden keine Punkte eingezeichnet
                        MarkerOptions marker = new MarkerOptions(); // neuer Marker, kompliziert, da Google und Mapbox beide eine LatLng klasse beistzen, deswegen über double Listen
                        for (int x = 0; x < onlyNearGeofencesLatArray.length; x++){
                            marker.position(new LatLng(onlyNearGeofencesLatArray[x], onlyNearGeofencesLonArray[x]));
                            mapboxMap.addMarker(marker);
                        }
                        whatPointsShown.setText("In dieser Ansicht werden alle in deiner Nähe befindlichen Kameras angezeigt");
                    }
                }
            }
        });

    }





    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Wir brauchen deine Zustimmung, sonst wirst du unwissend überwacht", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            if (mapboxMap.getStyle() != null) {
                enableLocationComponent(mapboxMap.getStyle());
            }
        } else {
            Toast.makeText(this, "Wir brauchen deine Zustimmung, sonst wirst du unwissend überwacht", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static class MainActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainActivity> activityWeakReference;

        MainActivityLocationCallback(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                // Create a Toast which displays the new location's coordinates
                /*Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
                        String.valueOf(result.getLastLocation().getLatitude()), String.valueOf(result.getLastLocation().getLongitude())),
                        Toast.LENGTH_SHORT).show();*/

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }

        }



        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }



    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
