package com.uwi.capstone.findthatspeaker;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class PersonActivity extends AppCompatActivity {

    private static final String LOG_TAG = "Resp_log";
    private static final String LOG_TAG_EX = "Extra";

    private final String MAINPAGE = "https://en.wikipedia.org/wiki/";
    private final String APIINFOQUERY = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=";
    private final String APIIMAGEQUERY = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&titles=";

    FloatingActionButton fab;
    ImageView img;
    TextView title, desc;
    ScrollView descScroll;
    CardView proPic;
    ProgressBar pd;

    private RequestQueue rQueue;
    private StringRequest request;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        img = (ImageView) findViewById(R.id.imageView);
        title = (TextView) findViewById(R.id.titleText);
        desc = (TextView) findViewById(R.id.descText);
        descScroll = (ScrollView) findViewById(R.id.descScroll);
        proPic = (CardView) findViewById(R.id.proPicView);
        pd = (ProgressBar) findViewById(R.id.progressBar);

        //Allows for scrolling.
        descScroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
                p.setAnchorId(View.NO_ID);
                fab.setLayoutParams(p);
                fab.setVisibility(View.GONE);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        fab.requestLayout();
                        fab.setVisibility(View.VISIBLE);
                    }
                }, 3000);
            }
        });

//        final String person = getIntent().getStringExtra("com.uwi.capstone.PERSON");
        //Log.i(LOG_TAG_EX, person);

        final String userData = getIntent().getStringExtra("com.uwi.capstone.USERDATA");
        Log.i(LOG_TAG_EX, userData);
        String[] data = userData.split("\\|");
        final String name = data[0];
        String description = data[1];

        new JsonTask().execute(data[2]);

        title.setText(name);
        desc.setText(description);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webview = new WebView(getApplicationContext());
                setContentView(webview);
                //webview.loadUrl(MAINPAGE + person);
                webview.loadUrl(MAINPAGE + name);
            }
        });

        //getWikiData(APIIMAGEQUERY + person, APIINFOQUERY + person);
    }

    public void getWikiData(String imgQuery, String contentQuery){
        rQueue = Volley.newRequestQueue(this);
        request = new StringRequest(Request.Method.GET, imgQuery, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response != null) {
                    try{
                        JSONObject json = new JSONObject(response);
                        JSONObject imgObj = json.getJSONObject("query").getJSONObject("pages");
                        Iterator<String> keySet = imgObj.keys();
                        String imgUrl = imgObj.getJSONObject(keySet.next()).getJSONObject("thumbnail").getString("original");
                        Log.i(LOG_TAG, imgUrl);
                        new JsonTask().execute(imgUrl);
                    }catch (Exception err){
                        Log.e(LOG_TAG, "" + err);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                    Log.e(LOG_TAG, error.toString());
            }
        });
        rQueue.add(request);
        request = new StringRequest(Request.Method.GET, contentQuery, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONObject json = new JSONObject(response);
                    JSONObject pageObj = json.getJSONObject("query").getJSONObject("pages");
                    JSONObject contentObj = pageObj.getJSONObject(pageObj.keys().next());
                    String title = contentObj.getString("title");
                    String desc = contentObj.getString("extract");
                    setContentFromJSON(title, desc);
                }catch (Exception err) {
                    Log.e(LOG_TAG, "" + err);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, error.getMessage());
            }
        });
        rQueue.add(request);
    }

    public void setContentFromJSON(String stringTitle, String stringDesc) {
        if(stringTitle.isEmpty() || stringTitle.equals("")) {
            title.setText("Unknown");
        }else{
            title.setText(stringTitle);
        }

        if(stringDesc.isEmpty() || stringDesc.equals("")) {
            desc.setText("Information not found.");
        }else{
            desc.setText(stringDesc);
        }
    }

    private class JsonTask extends AsyncTask<String, String, Bitmap> {

        protected void onPreExecute() {
            super.onPreExecute();
            pd.setIndeterminate(true);
        }

        protected Bitmap doInBackground(String... params) {
            Bitmap image = getBitmap(params[0]);
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            pd.setVisibility(View.GONE);
            title.setVisibility(View.VISIBLE);
            desc.setVisibility(View.VISIBLE);
            descScroll.setVisibility(View.VISIBLE);
            proPic.setVisibility(View.VISIBLE);
            if(result != null ){
                img.setImageBitmap(result);
            }else{
                String imageDrawable = "@drawable/unknown";
                int imageResource = getResources().getIdentifier(imageDrawable, null, getPackageName());
                Drawable res = getResources().getDrawable(imageResource);
                img.setImageDrawable(res);
            }
        }

        Bitmap getBitmap(String url){
            try {
                Log.e("src",url);
                URL src = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) src.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                Log.e("Bitmap","returned");
                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Exception",e.getMessage());
                return null;
            }
        }
    }
}
