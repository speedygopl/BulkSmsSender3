package com.example.bulksmssender3;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    Button sendBtn;
    EditText txtMessage;
    String message;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    PendingIntent sentPI, deliveredPI;
    BroadcastReceiver smsSentReceiver, smsDeliveredReceiver;
    SmsManager smsManager = SmsManager.getDefault();

    @Nullable
    @Override
    public File getExternalFilesDir(@Nullable String type) {
        return super.getExternalFilesDir(type);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ConstraintLayout constraintLayout = findViewById(R.id.main_layout);
        constraintLayout.setBackgroundColor(Color.GRAY);
        sendBtn = findViewById(R.id.btnSendSMS);
        txtMessage = findViewById(R.id.editText2);
        sendBtn.setOnClickListener(view -> {
            try {
                sendSMS();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), PendingIntent.FLAG_IMMUTABLE);
        deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        smsSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS sent OK", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(MainActivity.this, "Generic Failure Error", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(MainActivity.this, "No Service Error", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(MainActivity.this, "Null PDU Error", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(MainActivity.this, "Radio Off Error", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        smsDeliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS delivered OK", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(MainActivity.this, "SMS delivery Error", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        registerReceiver(smsSentReceiver, new IntentFilter(SENT));
        registerReceiver(smsDeliveredReceiver, new IntentFilter(DELIVERED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(smsDeliveredReceiver);
        unregisterReceiver(smsSentReceiver);
    }

    protected void sendSMS() throws FileNotFoundException {
        message = txtMessage.getText().toString();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        } else
            SendTextMsg();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        SendTextMsg();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "SMS failed", Toast.LENGTH_LONG).show();
                }
            }

        }
    }


    private void SendTextMsg() throws FileNotFoundException {
        List<String> phones = new ArrayList<>();
        phones.add("48501474399");
        phones.add("48518970434");
        File path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, "/" + "output.txt");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        for (String number : phones) {
            smsManager.sendTextMessage(number, null, message, sentPI, deliveredPI);
            System.out.println("smsSentReceiver code = " + smsSentReceiver.getResultCode());
            System.out.println("smsDeliveredReceiver code = " + smsDeliveredReceiver.getResultCode());
            System.out.println("Activity.RESULT_OK = " + Activity.RESULT_OK);
        }
    }
}
