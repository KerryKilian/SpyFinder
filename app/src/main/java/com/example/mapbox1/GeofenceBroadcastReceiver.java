package com.example.mapbox1;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceBroadcastReceiv";
    int geofence;

    @Override
    public void onReceive(Context context, Intent intent) {


        NotificationHelper notificationHelper = new NotificationHelper(context);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()){
            Log.d(TAG, "onReceive: Error receiving geofence event...");
            return;
        }


        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
        for (Geofence geofence: geofenceList) {
            Log.d(TAG, "onReceive: " + geofence.getRequestId());
        }
//        Location location = geofencingEvent.getTriggeringLocation();
        int transitionType = geofencingEvent.getGeofenceTransition();

        // Geofence Status
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                notificationHelper.sendHighPriorityNotification("Vorsicht: Kamera", "Du näherst dich einer Überwachungskamera", HauptActivity.class);
                geofence = 1;
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                notificationHelper.sendHighPriorityNotification("Vorsicht: Kamera", "Du bist in der Nähe einer Überwachungskamera", HauptActivity.class);
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT: // change textView here
                notificationHelper.sendHighPriorityNotification("Gefahr vorüber", "In der Nähe sind keine Kameras", HauptActivity.class);
                geofence = 0;
                break;
        }


        // intent, um Status des Geofence an HauptActivity zu übergeben
        Intent intentForTextView = new Intent("my-permission-response-action");
        intentForTextView.putExtra("geofence", geofence);


        // Broadcast to any registered receivers
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentForTextView);
    }



}