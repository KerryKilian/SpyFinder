package com.example.mapbox1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.BoundingBox;

import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;





public class HauptActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LocationListener { // OnMapReadyCallback


    // zur Orientierung: HauptActivity ist der Startbildschirm, MainActivity die Mapbox Karte (ich weiß, schlechte Namen)


    Button button;
    Button infoButton;
    Button startButton;
    Spinner spinner;
    private AlertDialog dialog;
    String item;
    LocationManager locationManager;
    TextView status;
    private FusedLocationProviderClient mFusedLocationClient;
    int geofence;

    private static final String TAG = "MapsActivity";

    private GoogleMap mMap;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;
    private float GEOFENCE_RADIUS = 200;
    private String GEOFENCE_ID = "SOME_GEOFENCE_ID"; // Wenn ein neuer geofence erstellt wird, dann wird ID überschrieben
    private List<LatLng> dangerousArea = new ArrayList<>();
    private List<LatLng> onlyNearGeofences = new ArrayList<>();
    private List<LatLng> test = new ArrayList<>();
    public List<Double> onlyNearGeofencesLat = new ArrayList<>();
    public List<Double> onlyNearGeofencesLon = new ArrayList<>();
    public List<Double> dangerousAreaLat = new ArrayList<>();
    public List<Double> dangerousAreaLon = new ArrayList<>();
    double[] onlyNearGeofencesLatArray;
    double[] onlyNearGeofencesLonArray;
    double[] dangerousAreaLatArray;
    double[] dangerousAreaLonArray;
    ConstraintLayout constraintLayout;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_haupt);

        status = (TextView) findViewById(R.id.status);
        status.setText("Keine Kameras in der Nähe");

        // zum Ändern des Hintergrunds später
        constraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);

        // Nach Standort fragen (Ab Android 10 muss beim Geofencing Background Location genutzt werden)
        if (Build.VERSION.SDK_INT >= 29){
            if (ContextCompat.checkSelfPermission(HauptActivity.this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(HauptActivity.this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    ActivityCompat.requestPermissions(HauptActivity.this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                }else{
                    ActivityCompat.requestPermissions(HauptActivity.this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
                }
            }
        }
        // Nach Standort fragen (Unter Android 10 muss beim Geofencing Fine Location genutzt werden)
        else{
            if (ContextCompat.checkSelfPermission(HauptActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(HauptActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)){
                    ActivityCompat.requestPermissions(HauptActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }else{
                    ActivityCompat.requestPermissions(HauptActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }
        }


        // Beim App-Start soll Location vom fusedLocationProviderClient geholt werden, da getLocation() vom LocationListener unzuverlässig funktioniert
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);






        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {

                if (item == "Wähle eine Region aus"){
                    dialog = new AlertDialog.Builder(HauptActivity.this) // App muss neu gestartet werden
                            .setTitle("Hoppla")
                            .setMessage("Wähle zuerst dein Bundesland aus. Danach kannst du auf Start klicken.")
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

                else {

                    // fusedLocationProviderClient nimmt beim Öffnen den Userstandort
                    // getLocation() vom LocationListener nimmt dann in bestimmten Abständen (30 Sek. und 100 m) den Userstandort

                    mFusedLocationClient.getLastLocation().addOnSuccessListener(HauptActivity.this, location -> {
                                if (location != null) {

                                    // für jede Koordinate werden diejenigen in die Liste "onlyNearGeofences" getan, die sich in der Nähe befinden
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        dangerousArea.forEach(coordinate -> {

                                        double lat2 = 0;
                                        double lon2 = 0;
                                        Pattern pattern = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
                                        Matcher matcher = pattern.matcher(coordinate.toString());
                                        while (matcher.find()) {

                                            String[] str = matcher.group(1).split(",");
                                            lat2 = Double.parseDouble(str[0]);
                                            lon2 = Double.parseDouble(str[1]);
                                        }

                                        //distance=Math.sqrt(((lat2 - currentLat) * (lat2 - currentLat)) + ((lon2 - currentLon) * (lon2 - currentLon)))*100000;// Meter
                                        if (lat2 > (location.getLatitude() - 0.01) & lat2 < (location.getLatitude() + 0.01)) {       // wenn Koordinate in einem bestimmten Bereich (keine Distanzrechnung, da diese rechenaufwändiger ist)
                                            if (lon2 > (location.getLongitude() - 0.01) & lon2 < (location.getLongitude() + 0.01)) {
                                                if (onlyNearGeofences.contains(coordinate)) {
                                                }          // wenn Koordinate schon in Liste enthalten ist, dann nichts machen
                                                else {                                                  // wenn Koordinate noch nicht in Liste enthalten ist, dann hinzufügen
                                                    onlyNearGeofences.add(new LatLng(lat2, lon2));
                                                }
                                            } else {
                                                if (onlyNearGeofences.contains(coordinate))
                                                    onlyNearGeofences.remove(coordinate);
                                                else {
                                                }
                                            }

                                        } else {
                                            if (onlyNearGeofences.contains(coordinate))
                                                onlyNearGeofences.remove(coordinate);
                                            else {
                                            }
                                        }
                                        ;
                                    });
                                }

                            }

                        // auf Grund der Problematik, dass sowohl Google als auch Mapbox die Klasse LatLng besitzen, konvertiere ich in 2 double Listen
                        convertLatLngToDouble(onlyNearGeofences);


                        // Konvertierung in double Listen, damit diese per intent in MainActivity übertragen werden können
                        onlyNearGeofencesLatArray = new double[onlyNearGeofencesLat.size()];
                        for (int x = 0; x < onlyNearGeofencesLat.size(); x++){
                            onlyNearGeofencesLatArray[x] = onlyNearGeofencesLat.get(x);
                        }

                        onlyNearGeofencesLonArray = new double[onlyNearGeofencesLon.size()];
                        for (int x = 0; x < onlyNearGeofencesLon.size(); x++){
                            onlyNearGeofencesLonArray[x] = onlyNearGeofencesLon.get(x);
                        }


                        // Erstelle Geofence um Kameras, die sich in der Nähe befinden (für alle Koordinaten, die sich in Liste "onlyNearGeofence" befinden)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onlyNearGeofences.forEach(coord -> {
                                tryAddingGeofence(coord);
                            });
                        }


                    });

                    // in regelmäßigen Abständen (30 Sek, 100 m) wird Standort gesucht
                    getLocation();

                    // Ladebalken von 3 Sekunden, da es in der Praxis einige Sekunden dauert, bis die Listen und Arrays erstellt wurden
                    ProgressDialog progressDialog = new ProgressDialog(HauptActivity.this);
                    progressDialog.setMessage("Bitte warte einige Sekunden, bis die Entfernungen zu den Kameras berechnet wurden."); // Setting Message
                    progressDialog.setTitle("Fast geschafft"); // Setting Title
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
                    progressDialog.show(); // Display Progress Dialog
                    progressDialog.setCancelable(false);
                    new Thread(new Runnable() {
                        public void run() {

                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            progressDialog.dismiss();
                        }
                    }).start();
                }

            }
        });


        // Button, um Karte zu öffnen
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMainActivity();
            }
        });

        // Button, um Information anzuzeigen
        infoButton = (Button) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = new AlertDialog.Builder(HauptActivity.this)
                        .setTitle("Wie funktioniert die App?")
                        .setMessage("Dein Standort und alle in der Nähe befindlichen Überwachungskameras aus deiner Region werden lokalisiert.\n" +
                                "Sobald du in die Nähe einer Kamera kommst, erhälst du die Meldung, dass du überwacht werden könntest.\n\n" +
                                "\nDie Standorte der Überwachungskameras wurden von der OpenStreetMap genommen. Es besteht keine Haftung für Richtigkeit. Wenn du bemerkst, dass eine Kamera fehlt,\n" +
                                "kannst du die Daten jederzeit kostenlos in der OpenStreetMap verändern.\n\n" +
                                "Laut Google kann es einige Minuten dauern, bis du die Benachrichtigung erhälst. Schalte dein WLAN an, um eine große Latenz zu verhindern. Möglicherweise musst du dich auch etwas bewegen, um eine Benachrichtigung zu erhalten.")
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

        // Spinner zur Auswahl des Bundeslandes
        spinner = (Spinner) findViewById(R.id.standortwahl);
        spinner.setOnItemSelectedListener(this);
        List<String> categories = new ArrayList<String>();
        categories.add("Wähle eine Region aus");
        categories.add("Berlin");
        categories.add("Bayern");
        categories.add("Brandenburg");
        categories.add("Mecklenburg-Vorpommern");
        categories.add("Sachsen");
        categories.add("Saarland");
        categories.add("Rheinland-Pfalz");
        categories.add("Baden-Württemberg");
        categories.add("Nordrhein-Westfalen");
        categories.add("Hessen");
        categories.add("Niedersachsen");
        categories.add("Bremen");
        categories.add("Schleswig-Holstein");
        categories.add("Hamburg");
        categories.add("Sachsen-Anhalt");
        categories.add("Thüringen");
        //categories.add("testlayer");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, categories);
        dataAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(dataAdapter);



        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

    }

    // Wenn vom Spinner ein Bundesland ausgewählt wurde
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { // was passiert, wenn Region über Spinner ausgewählt wurde
        item = parent.getItemAtPosition(position).toString();
        ProgressDialog progressDialog = new ProgressDialog(HauptActivity.this);
        progressDialog.setMessage("Lese die Koordinaten der Überwachungskameras aus...\nFür große Bundesländer kann dieser Prozess einige Sekunden dauern."); // Setting Message
        progressDialog.setTitle("Laden"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.show(); // Display Progress Dialog
        progressDialog.setCancelable(false);

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Koordinaten aus der Datei auslesen, die zum ausgewählten Bundesland passt
                    readCoordinatesFromKml();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
            }
        }).start();
    }
    public void onNothingSelected(AdapterView<?> arg0) {
        item ="Wähle eine Region aus";
    }











    // in bestimmten Abständen (30 Sek., 100 m) Standort erhalten
    void getLocation() {
        try {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 100, this); // es müssen mind. 30 Sek. vergangen und 100 Meter gelaufen sein, bis Standpunkte aktualisiert werden
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 100, this);


        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    // Wenn Standort verändert wurde:
    @Override
    public void onLocationChanged(Location location) {


                // füge alle Koordinaten, die sich in der Nähe befinden, zu Liste "onlyNearGeofences"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dangerousArea.forEach(coordinate -> {

                        double lat2 = 0;
                        double lon2 = 0;
                        Pattern pattern = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(coordinate.toString());
                        while (matcher.find()) {

                            String[] str = matcher.group(1).split(",");
                            lat2 = Double.parseDouble(str[0]);
                            lon2 = Double.parseDouble(str[1]);
                        }

                        if (lat2 > (location.getLatitude() - 0.01) & lat2 < (location.getLatitude() + 0.01)) {       // wenn Koordinate in einem bestimmten Bereich (keine Distanzrechnung, da diese rechenaufwändiger ist)
                            if (lon2 > (location.getLongitude() - 0.01) & lon2 < (location.getLongitude() + 0.01)) {
                                if (onlyNearGeofences.contains(coordinate)) {
                                }          // wenn Koordinate schon in Liste enthalten ist, dann nichts machen
                                else {                                                  // wenn Koordinate noch nicht in Liste enthalten ist, dann hinzufügen
                                    onlyNearGeofences.add(new LatLng(lat2, lon2));
                                }
                            } else {
                                if (onlyNearGeofences.contains(coordinate))
                                    onlyNearGeofences.remove(coordinate);
                                else {
                                }
                            }

                        } else {
                            if (onlyNearGeofences.contains(coordinate))
                                onlyNearGeofences.remove(coordinate);
                            else {
                            }
                        }
                        ;
                    });
                }

                // auf Grund der Problematik, dass sowohl Google als auch Mapbox die Klasse LatLng besitzen, konvertiere ich in 2 double Listen
                convertLatLngToDouble(onlyNearGeofences);


                // Konvertierung in double Listen, damit diese per intent in MainActivity (Karte) übertragen werden können
                onlyNearGeofencesLatArray = new double[onlyNearGeofencesLat.size()];
                for (int x = 0; x < onlyNearGeofencesLat.size(); x++){
                    onlyNearGeofencesLatArray[x] = onlyNearGeofencesLat.get(x);
                }

                onlyNearGeofencesLonArray = new double[onlyNearGeofencesLon.size()];
                for (int x = 0; x < onlyNearGeofencesLon.size(); x++){
                    onlyNearGeofencesLonArray[x] = onlyNearGeofencesLon.get(x);
                }


                // Erstelle Geofence um Punkte, die sich in Liste "onlyNearGeofences" befinden (also die Kameras, die sich in der Nähe befinden)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onlyNearGeofences.forEach(coord -> {
                        tryAddingGeofence(coord);
                    });
                }


    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(HauptActivity.this, "Please Enable GPS and Internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }




    // Radio-Buttons: Wie groß soll der Geofence-Radius sein?
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.funfzigMeter:
                if(checked)
                    GEOFENCE_RADIUS = 50;
                break;
            case R.id.hundertMeter:
                if(checked)
                    GEOFENCE_RADIUS = 100;
                break;
            case R.id.zweihundertMeter:
                if(checked)
                    GEOFENCE_RADIUS = 200;
                break;
        }
    }











    // Übergebe Koordinaten der Kameras an die Mapbox Karte und öffne sie anschließend
    public void openMainActivity(){
        Intent intent = new Intent (this, MainActivity.class);
        intent.putExtra("onlyNearGeofencesLatArray", onlyNearGeofencesLatArray);
        intent.putExtra("onlyNearGeofencesLonArray", onlyNearGeofencesLonArray);
        intent.putExtra("dangerousAreaLatArray", dangerousAreaLatArray);
        intent.putExtra("dangerousAreaLonArray", dangerousAreaLonArray);
        intent.putExtra("item", item);
        startActivity(intent);
    }




    // LatLng auf einzelne Koordinaten herunterbrechen und in 2 Listen abspeichern, da sowohl Google als auch Mapbox die Klasse LatLng besitzen (Fehler vermeiden)
    public void convertLatLngToDouble(List<LatLng> list){
        onlyNearGeofencesLat.clear();
        onlyNearGeofencesLon.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.forEach(coordinate -> {
                Pattern pattern = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(coordinate.toString());
                double lat = 0;
                double lon = 0;
                while (matcher.find()) {

                    String[] str = matcher.group(1).split(",");
                    lat = Double.parseDouble(str[0]);
                    lon = Double.parseDouble(str[1]);
                }

                onlyNearGeofencesLat.add(lat);
                onlyNearGeofencesLon.add(lon);
            });
        }


    }










    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // We have the permission
                mMap.setMyLocationEnabled(true);

            } else {
                // We dont have the permission
            }
        }
        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // We have the permission
                Toast.makeText(this,"Geofencing kann beginnen", Toast.LENGTH_SHORT).show();
            } else {
                // We dont have the permission
                Toast.makeText(this, "Wir brauchen deine Zustimmung, um dich vor der totalen Überwachung zu schützen", Toast.LENGTH_LONG).show();
            }
        }
    }


    // Aus interner Datei Koordinaten auslesen und in Listen speichern
    private void readCoordinatesFromKml() throws IOException {
        dangerousArea.clear();
        onlyNearGeofences.clear();
        onlyNearGeofencesLat.clear();
        onlyNearGeofencesLon.clear();
        dangerousAreaLat.clear();
        dangerousAreaLon.clear();

        String result = "";
        //InputStream inputStream = getResources().openRawResource(R.raw.monitoring);
        String string = "";
        StringBuilder stringBuilder = new StringBuilder();
        InputStream is;

        // Hier wird entschieden, welche Region behandelt werden soll (je nachdem, was beim Spinner ausgewählt wurde)

        if (item=="Brandenburg"){
            is = this.getResources().openRawResource(R.raw.brandenburg);
        }
        else if (item == "Mecklenburg-Vorpommern"){
            is = this.getResources().openRawResource(R.raw.mecklenburg_vorpommern);
        }
        else if (item == "Sachsen"){
            is = this.getResources().openRawResource(R.raw.sachsen);
        }
        else if (item == "Berlin"){
            is = this.getResources().openRawResource(R.raw.berlin);
        }
        else if (item == "Bayern"){
            is = this.getResources().openRawResource(R.raw.bayern);
        }
        else if (item == "Saarland"){
            is = this.getResources().openRawResource(R.raw.saarland);
        }
        else if (item == "Rheinland-Pfalz"){
            is = this.getResources().openRawResource(R.raw.rheinland_pfalz);
        }
        else if (item == "Baden-Württemberg"){
            is = this.getResources().openRawResource(R.raw.baden_wurttemberg);
        }
        else if (item == "Nordrhein-Westfalen"){
            is = this.getResources().openRawResource(R.raw.nordrhein_westfalen);
        }
        else if (item == "Hessen"){
            is = this.getResources().openRawResource(R.raw.hessen);
        }
        else if (item == "Niedersachsen"){
            is = this.getResources().openRawResource(R.raw.niedersachsen);
        }
        else if (item == "Bremen"){
            is = this.getResources().openRawResource(R.raw.bremen);
        }
        else if (item == "Schleswig-Holstein"){
            is = this.getResources().openRawResource(R.raw.schleswig_holstein);
        }
        else if (item == "Hamburg"){
            is = this.getResources().openRawResource(R.raw.hamburg);
        }
        else if (item == "Sachsen-Anhalt"){
            is = this.getResources().openRawResource(R.raw.sachsen_anhalt);
        }
        else if (item == "Thüringen"){
            is = this.getResources().openRawResource(R.raw.thuringen);
        }
        else if (item == "testlayer"){
            is = this.getResources().openRawResource(R.raw.testlayer);
        }
        else{
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (true) {
            try {
                if ((string = reader.readLine()) == null) break;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            stringBuilder.append(string).append("\n");
            result =""+stringBuilder;
        }
        is.close();

        Pattern pattern = Pattern.compile("<coordinates>(.*?)</coordinates>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(result);

        // für jede Koordinate, die der matcher findet:
        while (matcher.find()) {

            String[] str = matcher.group(1).split(","); // split in Lat und Lon
            double Longitude = Double.parseDouble(str[1]);
            double Latitude = Double.parseDouble(str[0]);

            // Lat und Lon in Listen hinzufügen
            dangerousAreaLat.add(Latitude);
            dangerousAreaLon.add(Longitude);
            dangerousArea.add(new LatLng(Latitude, Longitude));

        }

        // Arrays, damit Koordinaten per intent an "MapActivity" gegeben werden können (ich sehe keine Möglichkeit, Listen zu übergeben, deswegen Arrays)
        dangerousAreaLatArray = new double[dangerousArea.size()];
        dangerousAreaLonArray = new double[dangerousArea.size()];

        for (int x = 0; x < dangerousArea.size(); x++){
            dangerousAreaLatArray[x] = dangerousAreaLat.get(x);
            dangerousAreaLonArray[x] = dangerousAreaLon.get(x);
        }




    }




    // Erhalte Status vom GeofenceBraodcastReceiver (ENTER; EXIT), damit ich in HauptActivity Text entsprechend anzeigen lassen kann

    @Override
    public void onResume() {
        super.onResume();

        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // IntentFilter to define which actions mLocalBroadcastReceiver will respond to
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("my-permission-response-action");

        // Register for desired broadcasts
        localBroadcastManager.registerReceiver(mLocalBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {

        // Unregister our mLocalBroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
        super.onPause();
    }


    // Lokaler BroadcastReceiver, um Status des Geofence zu erhalten vom GeofenceBroadcastReceiver
    private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("my-permission-response-action")) {
                geofence = intent.getIntExtra("geofence", 3);


                // Wenn Kameras in der Nähe
                if (geofence == 1){
                    status.setText("Du näherst dich einer Überwachungskamera");
                    status.setGravity(Gravity.CENTER_HORIZONTAL);
                    status.setTextColor(Color.parseColor("#01579b"));
                    constraintLayout.setBackgroundColor(Color.parseColor("#FF8800"));




                }
                // wenn keine Kameras in der Nähe
                else if (geofence == 0){
                    status.setText("Keine Überwachungskamera in der Nähe");
                    status.setTextColor(Color.parseColor("#01579b"));
                    status.setGravity(Gravity.CENTER_HORIZONTAL);
                    constraintLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));


                }
                else{
                    status.setText("Nothing");
                }
            }
        }
    };











    private void tryAddingGeofence(LatLng latLng) {
        //mMap.clear();
        //addMarker(latLng);
        //addCircle(latLng, GEOFENCE_RADIUS);
        addGeofence(latLng, GEOFENCE_RADIUS);
    }


    @SuppressLint("MissingPermission")
    private void addGeofence(LatLng latLng, float radius){
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius,
                Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_DWELL |
                        Geofence.GEOFENCE_TRANSITION_EXIT);   // wann wird geofence getriggert? -> reinlaufen, darin laufen oder rausgehen
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();



        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence Added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);

                    }
                });
    }




}

