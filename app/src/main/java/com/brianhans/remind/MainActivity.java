package com.brianhans.remind;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int PICK_CONTACT = 10425;
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String RA_NAME = "raName";
    public static final String DISABLED = "disabled";
    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String PREFRENCE_NAME = "com.brianhans.remind.ra";
    public static final int ALARM_CODE = 2375;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    private Button timeButton;
    private Button contactButton;
    private Button disableButton;
    private TextView title;

    private String phoneNumber;
    private String raName;
    private boolean disabled;
    private int hour;
    private int minute;

    private View.OnClickListener timeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogFragment dialog = new TimePickerFragment();
            dialog.show(getSupportFragmentManager(), "timePicker");
        }
    };

    private View.OnClickListener contactListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                requestPermission(new String[]{Manifest.permission.READ_CONTACTS});
            }

            Intent contactPicker = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactPicker, PICK_CONTACT);
        }
    };

    private View.OnClickListener disableListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(disabled) {
                if(hour != -1 && minute != -1) {
                    Log.d(TAG, "onClick: !disabled: ran");
                    setAlarm(hour, minute);
                    disabled = false;

                    SharedPreferences.Editor edit = getSharedPreferences(PREFRENCE_NAME, Context.MODE_PRIVATE).edit();
                    edit.putBoolean(DISABLED, false);
                    edit.commit();

                    toggleDisabledButton();
                }else{
                    createAlert("Couldn't enable alarm", "Please enter a time");
                }
            }else {
                try {
                    cancelAlarm();

                    SharedPreferences.Editor edit = getSharedPreferences(PREFRENCE_NAME, Context.MODE_PRIVATE).edit();
                    edit.putBoolean(DISABLED, true);
                    edit.commit();

                    disabled = true;

                    toggleDisabledButton();
                } catch (Exception e) {
                    Log.e(TAG, "onClick: No Alarm", e);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeButton = (Button)findViewById(R.id.time_button);
        contactButton = (Button)findViewById(R.id.contact_button);
        disableButton = (Button)findViewById(R.id.disable_button);
        title = (TextView)findViewById(R.id.title);

        timeButton.setOnClickListener(timeListener);
        contactButton.setOnClickListener(contactListener);
        disableButton.setOnClickListener(disableListener);

        SharedPreferences sharedPref = getSharedPreferences(PREFRENCE_NAME,Context.MODE_PRIVATE);
        raName = sharedPref.getString(RA_NAME, "");
        phoneNumber = sharedPref.getString(PHONE_NUMBER, "");

        if(raName != "") title.setText("Contact " + raName);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},0
            );
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 0);
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED}, 0);
        }


        hour = sharedPref.getInt(HOUR, -1);
        minute = sharedPref.getInt(MINUTE, -1);
        disabled = sharedPref.getBoolean(DISABLED, true);


        Log.d(TAG, "onCreate: disabled:" + disabled);
        if(hour != -1 && minute != -1 && !disabled){
            setAlarm(hour, minute);
            toggleDisabledButton();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_CONTACT){
            if(resultCode == RESULT_OK) {
                Cursor cursor = getContentResolver().query(data.getData(), null, null, null, null);
                cursor.moveToFirst();

                String hasPhoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String phoneNumber = "";
                String name = "";

                if (hasPhoneNumber.equals("1")) {
                    Cursor phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null);
                    while (phone.moveToNext()) {
                        name = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[-() ]", "");
                        Log.i(TAG, "onActivityResult: " + phoneNumber + " " + name);
                    }
                    phone.close();

                    raName = name;
                    this.phoneNumber = phoneNumber;

                    SharedPreferences sharedPref = getSharedPreferences(PREFRENCE_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(RA_NAME, raName);
                    editor.putString(PHONE_NUMBER, this.phoneNumber);
                    editor.commit();

                    title.setText("Contact " + raName);

                } else {
                    Log.d(TAG, "onActivityResult: No Phone number");
                }
            }
        }
    }

    public void setTime(int hour, int minute){
        this.hour = hour;
        this.minute = minute;
    }

    private void cancelAlarm(){
        Intent intent = new Intent(getApplicationContext(), Alarm.class);
        PendingIntent alarm = PendingIntent.getBroadcast(getApplicationContext(), ALARM_CODE, intent, 0);
        AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        am.cancel(alarm);
    }

    public void setAlarm(int hour, int minute){
        cancelAlarm();

        //Get date for hour and minute
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if(calendar.HOUR_OF_DAY < hour){
            calendar.add(Calendar.DATE, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);


        //Set alarm
        Intent intent = new Intent(getApplicationContext(), Alarm.class);
        alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), ALARM_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY,alarmIntent);

        Log.d(TAG, "finished alarm");
    }

    public void toggleDisabledButton(){
        if(disableButton.getText().equals("Disable")) {
            disableButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            disableButton.setText("Enable");
        }else{
            disableButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            disableButton.setText("Disable");
        }
    }

    public void requestPermission(String[] permissions){
        ActivityCompat.requestPermissions(this,
               permissions,0
        );
    }

    public void createAlert(String title, String message){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("OK", null)
                .create()
                .show();
    }

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener{
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            SharedPreferences pref = getActivity().getSharedPreferences(PREFRENCE_NAME, Context.MODE_PRIVATE);
            int hour = pref.getInt(HOUR, -1);
            int minute = pref.getInt(MINUTE, -1);

            if(hour == -1 || minute == -1) {
                final Calendar cal = Calendar.getInstance();
                hour = cal.get(Calendar.HOUR_OF_DAY);
                minute = cal.get(Calendar.MINUTE);
            }

            return new TimePickerDialog(getActivity(), this, hour, minute, false);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            SharedPreferences.Editor edit = getActivity().getSharedPreferences(PREFRENCE_NAME, Context.MODE_PRIVATE).edit();
            edit.putInt(HOUR, hourOfDay);
            edit.putInt(MINUTE, minute);
            edit.commit();

            ((MainActivity)getActivity()).setTime(hourOfDay, minute);
        }
    }

}

