package com.brianhans.remind;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;


import java.io.IOException;
import java.util.List;

/**
 * Created by Brian on 7/1/16.
 */
public class Alarm extends BroadcastReceiver {

    private String raPhone = "";
    private String message = "";
    private Context context;

    public static final String TAG = Alarm.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "called");

        this.context = context;

        SharedPreferences sharedPref = context.getSharedPreferences(MainActivity.PREFRENCE_NAME,Context.MODE_PRIVATE);
        raPhone = sharedPref.getString(MainActivity.PHONE_NUMBER, "");


        if(raPhone != "") {

            try {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    handleNewLocation(location);
                } else {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                            .setContentTitle("RA Remind")
                            .setContentText("Couldn't get location")
                            .setSmallIcon(R.drawable.ic_stat_ic_launcher_app);

                    NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notifyManager.notify(002, builder.build());

                }
            } catch (SecurityException e) {
                Log.d(TAG, "onConnected: no permission");
            }
        }else{
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setContentTitle("Error Sending")
                    .setContentText("RA Number not set")
                    .setSmallIcon(R.drawable.ic_stat_ic_launcher_app);

            NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifyManager.notify(003, builder.build());
        }
    }


    private void handleNewLocation(Location location) {
        Geocoder geocoder = new Geocoder(context);
        try {
            List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            message = address.get(0).getAddressLine(0) + ", " + address.get(0).getAddressLine(1);
            String notificationText = "";

            float[] distance = new float[2];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), 37.776173, -122.418030, distance);

            if(distance[0] < 70){
                message = "I am in the dorms.";
                notificationText = "In Dorm";
            }else{
                message = "For some reason I'm not in the dorms. Here is my address: " + message;
                notificationText = "Not in dorm";
            }

            //Sends text to RA
            SmsManager.getDefault().sendTextMessage(raPhone, null, message, null, null);


            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setContentTitle("Message sent")
                    .setContentText(notificationText)
                    .setSmallIcon(R.drawable.ic_stat_ic_launcher_app);

            NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifyManager.notify(001, builder.build());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
