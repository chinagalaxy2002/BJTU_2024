package com.aiot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WebSocketClientManager {
    private WebSocket webSocket;
    private final OkHttpClient client = new OkHttpClient();

    // 回调接口，用于处理接收到的消息
    public interface Callback {
        void onReceived(byte[] data);
    }

    private Callback callback;

    public WebSocketClientManager(Callback callback) {
        this.callback = callback;
    }

    public void connect(String url) {
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                Log.d("WebSocket", "Connection opened");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                Log.d("WebSocket", "Text message received");

                try {
                    JSONObject jsonObject = new JSONObject(text);
                    String imageBase64 = jsonObject.getString("image");
                    JSONObject classCountsJson = jsonObject.getJSONObject("class_counts");

                    Map<String, Integer> classCounts = new HashMap<>();
                    Iterator<String> keys = classCountsJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        int value = classCountsJson.getInt(key);
                        classCounts.put(key, value);
                    }

                    byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                    final Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    EventBus.getDefault().post(new ImageEvent(decodedBitmap, classCounts));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                Log.e("WebSocket", "Error: " + t.getMessage());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                Log.d("WebSocket", "Connection closed: " + reason);
            }
        });
    }

    public void sendFrame(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] compressedData = baos.toByteArray();
            webSocket.send(ByteString.of(compressedData));
        } else {
            String textData = new String(data);
            webSocket.send(textData);
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Goodbye !");
        }
    }
}
