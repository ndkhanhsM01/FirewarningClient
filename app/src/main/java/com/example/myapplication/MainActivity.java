package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
                        implements MyWebSocketClient.WebSocketListener
{
    private MyWebSocketClient webSocketClient;
    private TextView messageTextView;
    private Button registBtn;
    private Handler uiHandler;
    private String clientId;

    public static String TAG = "Myapp_Test";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiHandler = new Handler(Looper.getMainLooper());
        SetupUIElements();
        ConnectWss();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    @Override
    public void onOpen() {
        // Handle WebSocket connection opened event
        Log.d(TAG, "WebSocket connection opened");
    }

    @Override
    public void onMessage(final String message) {
        // Handle WebSocket message received event
        Log.d(TAG, "WebSocket message received: " + message);

        HandleMessageReceived(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // Handle WebSocket connection closed event
        Log.d(TAG, "WebSocket connection closed. Code: " + code + ", Reason: " + reason + ", Remote: " + remote);
    }

    @Override
    public void onError(Exception ex) {
        // Handle WebSocket error event
        Log.e(TAG, "WebSocket error occurred", ex);
    }

    private  void HandleMessageReceived(String message){
        Gson gson = new Gson();
        InfoReceive infoReceive = gson.fromJson(message, InfoReceive.class);

        if(infoReceive == null){
            runOnUiThread(() -> messageTextView.setText("Messsage is null or empty"));
            return;
        }

        if(infoReceive.Type == InfoReceive.MessageType.MESSAGE)
        {
            runOnUiThread(() -> messageTextView.setText("message: " + infoReceive.Message));

            String newClientId = infoReceive.ClientId;
            if(newClientId != null && newClientId != ""){
                this.clientId = newClientId;
            }
        }
        else if(infoReceive.Type == InfoReceive.MessageType.WARNING){
            runOnUiThread(() -> messageTextView.setText("Warning: " + infoReceive.Message));
        }
    }

    private void SendRegistRequest(String jsonBody) {
        OkHttpClient client = new OkHttpClient();

        // URL of the API endpoint
        String url = ConfigVariable.apiRegist;

        // Create the JSON object to send in the request body
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        // Build the request
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // Send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "Post request failed", e);
                runOnUiThread(() -> messageTextView.setText("Regist failed!"));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    Log.d(TAG, "Post request successful: " + body);
                    HandleMessageReceived(body);
                } else {
                    Log.e(TAG, "Post request failed: " + response.code());
                    runOnUiThread(() -> messageTextView.setText("Regist failed!"));
                }
            }
        });
    }

    private  void ConnectWss(){
        try {
            // Replace "ws://your_server_address" with your actual WebSocket server address
            webSocketClient = new MyWebSocketClient(this, ConfigVariable.wssAddress);
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d(TAG, "connect faild");
        }
    }

    private void SetupUIElements(){
        messageTextView = findViewById(R.id.messageTextView);

        // button regist
        registBtn = findViewById(R.id.registBtn);
        registBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(clientId != null && clientId != ""){
                            SendRegistRequest("{\"clientId\":\"" + clientId + "\"}");
                        }
                    }
                }
        );
    }
}
