package com.example.myapplication;

import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;

public class MyWebSocketClient extends WebSocketClient {

    private static final String TAG = "WebSocketClient";
    private WebSocketListener listener;

    public MyWebSocketClient(WebSocketListener listener, String serverUrl) throws URISyntaxException {
        super(new URI(serverUrl));
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Connected to server");
        if (listener != null) {
            listener.onOpen();
        }
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Received message: " + message);
        if (listener != null) {
            listener.onMessage(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Disconnected from server. Code: " + code + ", Reason: " + reason + ", Remote: " + remote);
        if (listener != null) {
            listener.onClose(code, reason, remote);
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "Error occurred", ex);
        if (listener != null) {
            listener.onError(ex);
        }
    }

    public interface WebSocketListener {
        void onOpen();
        void onMessage(String message);
        void onClose(int code, String reason, boolean remote);
        void onError(Exception ex);
    }
}
