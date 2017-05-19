package com.uwi.capstone.findthatspeaker;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import services.WavAudioRecorder;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG_R = "Rec_Log";
    private static final String LOG_TAG_W = "Rec_Log";
    public static final String EXTRA_USERDATA = "com.uwi.capstone.USERDATA";

    ImageButton recordBtn;
    WavAudioRecorder rec;
    TextView recordLabel;
    MediaRecorder mRecorder;
    String file;
    int samplingRate;
    public ProgressDialog loadingdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for permissions
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // If we don't have permissions, ask user for permissions
        if (permission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS_STORAGE = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            int REQUEST_EXTERNAL_STORAGE = 1;

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        final String ip = getIntent().getStringExtra("com.uwi.capstone.IP");
        final int port = getIntent().getIntExtra("com.uwi.capstone.PORT", 20001);

        file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS; //Change to Internal Storage
        file += "/rec.wav";
        recordLabel = (TextView) findViewById(R.id.recordLabel);
        recordBtn = (ImageButton) findViewById(R.id.recordBtn);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordLabel.setText("Listening...");
                startRecordWav();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stop(0);
                        //queryServer("172.16.189.172", 20001);
                        queryServer(ip, port);
                    }
                }, 10000);
            }
        });
    }

    //Records WAV file with a custom recorder that utilizes AudioRecord
    private void startRecordWav(){
        rec = WavAudioRecorder.getInstanse();
        samplingRate = rec.getSamplingRate();
        rec.setOutputFile(file);
        try{
            rec.prepare();
        }catch (Exception e){
            Log.e(LOG_TAG_R, "prepare() failed");
            System.out.println(e);
        }

        rec.start();
    }

    private void stop(int toggle){
        try{
            if (toggle == 1){
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }else {
                rec.stop();
                rec.release();
                rec = null;
            }
        }catch (Exception e){

        }
        recordLabel.setText("Tap to Listen");
    }

    private void queryServer(final String url, final int port){
        loadingdialog = ProgressDialog.show(MainActivity.this,
                "Please Wait","Finding Celebrity...",true);
        Log.i(LOG_TAG_W, "Connecting...");

        //Thread used to communicate to Server via Sockets.
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS; //Change to Internal Storage
                filePath += "/rec.wav";
                int found = 0;  //Initialize to 0
                Socket sock;
                String response = "";
                try {
                    /*
                    Code that could be used for accessing the server as a hosted web service.

                    HttpClient httpclient = new DefaultHttpClient();

                    HttpPost httppost = new HttpPost(url);

                    InputStreamEntity reqEntity = new InputStreamEntity(
                            new FileInputStream(new File(file)), -1);
                    reqEntity.setContentType("binary/octet-stream");
                    reqEntity.setChunked(true); // Send in multiple parts if needed
                    httppost.setEntity(reqEntity);
                    HttpResponse response = httpclient.execute(httppost); */

                    //Set socket if found
                    sock = new Socket(url, port);
                    Log.i(LOG_TAG_W, "Connected");

                    // Initializes components for sending audio file.
                    File myFile = new File(filePath);
                    byte[] mybytearray = new byte[(int) myFile.length()];
                    FileInputStream fis = new FileInputStream(myFile);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(mybytearray, 0, mybytearray.length);
                    OutputStream os = sock.getOutputStream();

                    InputStream is = sock.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    while (true){
                        if(br.ready()){
                            response = br.readLine();
                            break;
                        }
                    }

                    Log.i(LOG_TAG_W, "Sending File with size: " + mybytearray.length + " ...");
                    os.write(mybytearray, 0, mybytearray.length);
                    os.write(("Done").getBytes());
                    os.flush();

                    Log.i(LOG_TAG_W, "Listening....");
                    String line;
                    response = "";
                    while (true){
                        if(br.ready()){
                            while((line = br.readLine()) != null){
                                response += line;
                            }
                            break;
                        }
                    }

                    Log.i(LOG_TAG_W, "Message received from the server : " + response); //Log the result from the request

                    sock.close();
                    found = 1;
                } catch (Exception e) {
                    // show error
                    Log.e(LOG_TAG_W, e.toString());
                }

                //Used for Wikipedia's API
                /*String person = "";
                try {
                    person = URLEncoder.encode(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }*/

                if (found == 1) {
                    //Start Activity to Display person if found.
                    Intent intent = new Intent(getApplicationContext(), PersonActivity.class);
                    intent.putExtra(EXTRA_USERDATA, response);
                    startActivity(intent);
                } else {
                    recordLabel.post(new Runnable() {
                        public void run() {
                            recordLabel.setText("Unable to Identify. Try again.");
                        }
                    });
                }
                loadingdialog.dismiss();
            }
        });

        thread.start();
    }
}
