package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.databinding.ActivityMainBinding;
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

public class MainActivity extends AppCompatActivity implements MyWebSocketClient.WebSocketListener {
    private MyWebSocketClient webSocketClient;
    private TextView messageTextView;
    private Handler uiHandler;
    private String clientId;
    private ActivityMainBinding binding;
    public static String TAG = "Myapp_Test";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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

    private void HandleMessageReceived(String message) {
        Gson gson = new Gson();
        InfoReceive infoReceive = gson.fromJson(message, InfoReceive.class);
        if (infoReceive == null) {
            runOnUiThread(() -> messageTextView.setText("Messsage is null or empty"));
            return;
        }
        if (infoReceive.Type == InfoReceive.MessageType.MESSAGE) {
            runOnUiThread(() -> messageTextView.setText("message: " + infoReceive.Message));
            String newClientId = infoReceive.ClientId;
            if (newClientId != null && newClientId != "") {
                this.clientId = newClientId;
            }
        } else if (infoReceive.Type == InfoReceive.MessageType.WARNING) {
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
        Request request = new Request.Builder().url(url).post(body).build();
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

    private void ConnectWss() {
        try {
            // Replace "ws://your_server_address" with your actual WebSocket server address
            webSocketClient = new MyWebSocketClient(this, ConfigVariable.wssAddress);
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d(TAG, "connect faild");
        }
    }

    private void SetupUIElements() {
        messageTextView = binding.messageTextView;
        // button regist
        binding.registBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //                        if (clientId != null && clientId != "") { //
                //                        SendRegistRequest("{\"clientId\"😕"" + clientId + "\"}"); //
                //                        }
                createAndShowNotification(getResources().getString(R.string.signs_of_fire_were_detected_check_now));
            }
        });
        binding.btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFireWarningPopup();
            }
        });
    }

    private void showFireWarningPopup() {
        runOnUiThread(() -> {
            View dialogView = getLayoutInflater().inflate(R.layout.popup_warning, null);
            // Create the dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(dialogView);
            builder.setCancelable(false);
            // Create and show the dialog
            AlertDialog dialog = builder.create();
            Button btnPopupOk = dialogView.findViewById(R.id.btnPopupOk);
            btnPopupOk.setOnClickListener(v -> dialog.dismiss());
            ImageView btnPopupClose = dialogView.findViewById(R.id.btnPopupClose);
            btnPopupClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        });
    }

    private void createAndShowNotification(String message) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        // Create the notification channel if necessary
        createNotificationChannel();
        // Build the notification

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "fire_warning_channel").setSmallIcon(R.drawable.warning)
                .setContentTitle("Fire Warning").setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_ALARM).setContentIntent(pendingIntent).setAutoCancel(true);
        // Dismiss the notification when clicked
        //
        //Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
        }
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Fire Warning Channel";
            String description = "Channel for fire warning notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("fire_warning_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this).setTitle("Notification Permission Required").setMessage("This app requires notification permissions to function properly. Please enable it in settings.").setPositiveButton("Settings", (dialog, which) -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }).setNegativeButton("Cancel", null).show();
    }
}