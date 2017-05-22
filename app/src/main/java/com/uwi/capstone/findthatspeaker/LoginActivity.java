package com.uwi.capstone.findthatspeaker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_PORT = "com.uwi.capstone.PORT";
    public static final String EXTRA_IP = "com.uwi.capstone.IP";


    TextView ipText, portText;
    Button connectBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ipText = (TextView) findViewById(R.id.txtIP);
        portText = (TextView) findViewById(R.id.txtPort);

        connectBtn = (Button) findViewById(R.id.btnConnect);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip;
                int port;
                ip = ipText.getText().toString();
                port = Integer.parseInt(portText.getText().toString());
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(EXTRA_IP, ip);
                intent.putExtra(EXTRA_PORT, port);
                startActivity(intent);
            }
        });

    }
}
