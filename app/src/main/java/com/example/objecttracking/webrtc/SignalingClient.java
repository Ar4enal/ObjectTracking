package com.example.objecttracking.webrtc;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class SignalingClient {

    private static SignalingClient instance;
    private SignalingClient(){
        init();
    }
    public static SignalingClient get() {
        if(instance == null) {
            synchronized (SignalingClient.class) {
                if(instance == null) {
                    instance = new SignalingClient();
                }
            }
        }
        return instance;
    }

    private Callback callback;
    //private final OkHttpClient client = new OkHttpClient();
    private OkHttpClient client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)//允许失败重试
            .readTimeout(5, TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(5, TimeUnit.SECONDS)//设置写的超时时间
            .connectTimeout(5, TimeUnit.SECONDS)//设置连接超时时间
            .build();
    private final String url ="ws://37.18.4.44:8080/ws";
    private WebSocket mWebSocket;


    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void init() {
        Request request = new Request.Builder().get().url(url).build();
        mWebSocket = client.newWebSocket(request, new SocketListener());
    }

    public void sendOfferSessionDescription(SessionDescription sdp) throws JSONException {
        JSONObject jo = new JSONObject();
        JSONObject offer_jo = new JSONObject();
        offer_jo.put("type", "offer");
        offer_jo.put("sdp", sdp.description);
        jo.put("event", "offer");
        jo.put("data",String.valueOf( offer_jo));

        sendMessage(String.valueOf(jo));
    }

    public void sendMessage(String message){
        Log.i("websocket_send_offer", message);
        mWebSocket.send(message);
    }

    class SocketListener extends WebSocketListener {
        private static final String TAG = "SocketListener";

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            Log.i(TAG,"onOpen response="+response);
            callback.onSelfJoined();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.i(TAG,"onMessage text="+text);
            try {
                JSONObject message = new JSONObject(text);
                if (message.getString("event").equals("answer")){
                    JSONObject jsonObject = new JSONObject(message.getString("data"));
                    callback.onAnswerReceived(jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
            Log.i((String) TAG,"onMessage bytes="+bytes);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            Log.i(TAG,"onClosing code="+code);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            Log.i(TAG,"onClosed code="+code);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            Log.i(TAG,"onFailure t="+t.getMessage());
        }
    }

    public interface Callback {
        void onSelfJoined();
        void onAnswerReceived(JSONObject data);
    }

}

