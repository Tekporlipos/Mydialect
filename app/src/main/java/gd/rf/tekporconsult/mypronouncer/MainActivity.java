package gd.rf.tekporconsult.mypronouncer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import gd.rf.tekporconsult.mypronouncer.CallBacks.JavaScriptCallBack;
import gd.rf.tekporconsult.mypronouncer.model.Transcribe;
import gd.rf.tekporconsult.mypronouncer.model.Trending;
import gd.rf.tekporconsult.mypronouncer.service.DatabaseAccess;

public class MainActivity extends AppCompatActivity {

    protected WebView webView;
    protected int a = 0;
    FloatingActionButton sharedValueSet;
    protected FirebaseFirestore db;
    protected String key;
    DatabaseAccess databaseAccess;
    final static int REQ_CODE_SPEECH_INPUT = 99;
    public static String defaultLang = "dictionary";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);




        sharedValueSet = findViewById(R.id.sharedValueSet);


        sharedValueSet.setOnClickListener(view -> {
            webView.evaluateJavascript("javascript:startShare()", s ->
                    {
//                                                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
            );
        });


        db = FirebaseFirestore.getInstance();
        databaseAccess = DatabaseAccess.getInstance(MainActivity.this);
        databaseAccess.open();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webView.getSettings().setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.red));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.redPrimary));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webSettings.setOffscreenPreRaster(true);
        }
        webSettings.setAppCacheEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webSettings.setForceDark(WebSettings.FORCE_DARK_AUTO);
        }
        webSettings.setNeedInitialFocus(true);

        webView.loadUrl("file:///android_asset/index.html");


        db.collection("api").document("dictionary")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot result = task.getResult();

                        webView.addJavascriptInterface(new JavaScriptCallBack(MainActivity.this, webView, db, databaseAccess,String.valueOf(result.get("app_id")),String.valueOf(result.get("app_key"))), "Android");


                    }
                });

        db.collection("api").document("location")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot result = task.getResult();
                        key = String.valueOf(result.get("key"));

                    }
                });


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request.getUrl() != null && (request.getUrl().toString().startsWith("http://") || request.getUrl().toString().startsWith(("https://")))) {
                        view.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(request.getUrl().toString())));
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (view.getUrl() != null && (view.getUrl().startsWith("http://") || view.getUrl().startsWith(("https://")))) {
                        view.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(view.getUrl())));
                        return true;
                    } else {
                        return false;
                    }
                }
            }


            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                view.loadUrl("file:///android_asset/home.html");
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);


                if(url.contains("lookup")){
                    sharedValueSet.setVisibility(View.VISIBLE);
                }else{
                    sharedValueSet.setVisibility(View.GONE);
                }




                if (url.contains("home")) {
                    view.clearHistory();

                    db.collection(defaultLang).orderBy("date", Query.Direction.DESCENDING).limit(1)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    ArrayList<Object> arrayList = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Map<String, Object> data = document.getData();
                                        arrayList.add(data);

                                        db.collection("pronunciation").document(document.getId())
                                                .get()
                                                .addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        ArrayList<Object> arrayList1 = new ArrayList<>();
                                                        DocumentSnapshot result = task1.getResult();
                                                        Map<String, Object> objectMap = new HashMap<>();
                                                        objectMap.put("id", result.getId());
                                                        objectMap.put("phoneticSpelling", result.get("phoneticSpelling"));

                                                        arrayList1.add(objectMap);

                                                        String jsonText1 = JSONValue.toJSONString(arrayList1);
                                                        webView.evaluateJavascript("javascript:pronounce(" + jsonText1 + ")", s ->
                                                                {
//                                                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                                                                }
                                                        );

                                                    }
                                                });
                                    }


                                    String jsonText = JSONValue.toJSONString(arrayList);
                                    webView.evaluateJavascript("javascript:recent(" + jsonText + ")", s ->
                                            {
//                                                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                                            }
                                    );


                                }
                            });


                    db.collection(defaultLang).orderBy("view", Query.Direction.DESCENDING).limit(15)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    ArrayList<Object> arrayList = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Map<String, Object> data = document.getData();
                                        arrayList.add(data);
                                    }

                                    String jsonText = JSONValue.toJSONString(arrayList);
                                    webView.evaluateJavascript("javascript:trending(" + jsonText + ")", s ->
                                            {
//                                                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                                            }
                                    );

                                }
                            });

                    ArrayList<Map<String, String>> arrayList = new ArrayList<>();
                    Map<String,String> stringStringMap = new HashMap<>();
                    stringStringMap.put("key",key);
                    arrayList.add(stringStringMap);

                    String jsonText = JSONValue.toJSONString(arrayList);

                    webView.evaluateJavascript("javascript:getLocation(" + jsonText + ")", s ->
                            {
//                                                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                            }
                    );



                } else if (url.contains("bookmark")) {


                    ArrayList<Trending> bookmark = databaseAccess.getBookmark();
                    ArrayList<Object> arrayList = new ArrayList<>();

                    for (Trending trending : bookmark) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", trending.getWord());
                        data.put("definition", trending.getDefinition());
                        arrayList.add(data);
                    }
                    String jsonText = JSONValue.toJSONString(arrayList);
                    webView.evaluateJavascript("javascript:getHistory(" + jsonText + ")", s ->
                            {
//                                                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                            }
                    );
                } else if (url.contains("history")) {


                    ArrayList<Trending> bookmark = databaseAccess.getHistory();
                    ArrayList<Object> arrayList = new ArrayList<>();

                    for (Trending trending : bookmark) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", trending.getDefinition());
                        data.put("definition", trending.getWord());
                        arrayList.add(data);
                    }
                    String jsonText = JSONValue.toJSONString(arrayList);
                    webView.evaluateJavascript("javascript:getHistory(" + jsonText + ")", s ->
                            {
//                                                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                            }
                    );
                } else if (url.contains("translate")) {

                    ArrayList<Transcribe> bookmark = databaseAccess.getTranscribe();
                    ArrayList<Object> arrayList = new ArrayList<>();

                    for (Transcribe transcribe : bookmark) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", transcribe.getId());
                        data.put("fromLang", transcribe.getFromLang());
                        data.put("fromKey", transcribe.getFromKey());
                        data.put("message", transcribe.getMessage());
                        data.put("toLang", transcribe.getToLang());
                        data.put("toKey", transcribe.getToKey());
                        arrayList.add(data);
                    }
                    String jsonText = JSONValue.toJSONString(arrayList);
                    webView.evaluateJavascript("javascript:getTranscribe(" + jsonText + ")", s ->
                            {
//                                                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                            }
                    );

                }


            }
        });

        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        Uri appLinkData = appLinkIntent.getData();

        if(appLinkData != null){
            if(!String.valueOf(appLinkData).isEmpty()){
                String path = appLinkData.toString();
                int i = path.indexOf('=');
                String substring = path.substring(i+1);
                webView.loadUrl("file:///android_asset/home.html?word="+substring);
            }
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            }
        }


    }


    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            if(sharedText.split("[\n ]").length < 4){
                webView.loadUrl("file:///android_asset/home.html?word="+sharedText.replaceAll("[\n]", " "));
            }else{

                webView.loadUrl("file:///android_asset/home.html?word="+sharedText.split("[\n ]")[0]);
            }
        }
    }

    public static void setDefaultLang(String value){
        defaultLang = value;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();

            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);

    }


    @Override
    public void onBackPressed() {
        if (a > 2) {
            super.onBackPressed();
        } else {
            a++;
        }

    }


    static public void startListening(Activity activity) {
//Intent to listen to user vocal input and return result in same activity
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //Use a language model based on free-form speech recognition.
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        //Message to display in dialog box
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, activity.getString(R.string.speech_to_text_info));
        try {
            activity.startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            a.fillInStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Toast.makeText(this, result.get(0), Toast.LENGTH_SHORT).show();

                    ArrayList<Object> arrayList = new ArrayList<>();
                    Map<String, Object> dataList = new HashMap<>();
                    dataList.put("text", result.get(0));
                    arrayList.add(dataList);
                    String jsonText = JSONValue.toJSONString(arrayList);
                    webView.evaluateJavascript("javascript:listingCallBack(" + jsonText + ")", s ->
                            {
//                                                    Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
                            }
                    );
                }
                break; }
        } }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseAccess.close();
    }
}