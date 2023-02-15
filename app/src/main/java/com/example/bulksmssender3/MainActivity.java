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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    Button sendBtn;
    EditText txtMessage;
    String message;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    PendingIntent sentPI, deliveredPI;
    BroadcastReceiver smsSentReceiver, smsDeliveredReceiver;
    SmsManager smsManager = SmsManager.getDefault();
    List<String> receivedCodes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ConstraintLayout constraintLayout = findViewById(R.id.main_layout);
        constraintLayout.setBackgroundColor(Color.GRAY);
        sendBtn = findViewById(R.id.btnSendSMS);
        txtMessage = findViewById(R.id.editText2);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendSMS();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
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

    public void receiverPrintResultCodeToFile() throws IOException {
        String text = "";
        for (String r : receivedCodes) {
            System.out.println(r);
            text = text + " " + r;
        }
        FileOutputStream fileOutputStream = openFileOutput("output1.txt", MODE_PRIVATE);
        fileOutputStream.write(text.getBytes());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(smsDeliveredReceiver);
        unregisterReceiver(smsSentReceiver);
    }

    protected void sendSMS() throws IOException, InterruptedException {
        message = txtMessage.getText().toString();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.SEND_SMS, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            SendTextMsg();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                SendTextMsg();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            Toast.makeText(getApplicationContext(), "SMS failed", Toast.LENGTH_LONG).show();
        }
    }


    private void SendTextMsg() throws IOException, InterruptedException {
        List<String> phones = readFileFromUri();
        for (String number : phones) {
            smsManager.sendTextMessage(number, null, message, sentPI, deliveredPI);
            TimeUnit.SECONDS.sleep(2);
        }
    }

    public List<String> readListOfPhoneNumbers() {
        List<String> phones = new ArrayList<>();
        try {
            FileInputStream fIn = openFileInput("input.txt");
            InputStreamReader isr = new InputStreamReader(fIn);
            BufferedReader buffreader = new BufferedReader(isr);

            String readString = buffreader.readLine();
            while (readString != null) {
                phones.add(readString);
                readString = buffreader.readLine();
            }

            isr.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return phones;
    }

    private List<String> readTextFromUri() throws IOException {
        List<String> phones = new ArrayList<>();
        String dirDocPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        File file = new File(dirDocPath
                + File.separator + "input.txt");
        Uri path = Uri.fromFile(file);
        try (InputStream inputStream =
                     getContentResolver().openInputStream(path);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                phones.add(line);
            }
        }
        return phones;
    }

    private List<String> readFileFromUri() throws IOException {
        List<String> phones = new ArrayList<>();
        String dirDownloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        String dirDownloadPathSelf = "storage/self/primary/Download";
        File file = new File(dirDownloadPathSelf
                + File.separator + "input.txt");
        Uri uri = Uri.fromFile(file);
        String readOnlyMode = "r";
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, readOnlyMode);
            InputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(Objects.requireNonNull(inputStream)));
            String line;
            while ((line = reader.readLine()) != null) {
                phones.add(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e + " " + uri);
        }
        return phones;
    }

}

