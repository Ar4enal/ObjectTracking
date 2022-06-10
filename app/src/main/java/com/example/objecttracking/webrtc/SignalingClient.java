package com.example.objecttracking.webrtc;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import org.json.JSONException;
import com.alibaba.fastjson.JSONObject;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
    private String localID;
    private String peerID;
    private final OkHttpClient client = new OkHttpClient();
    private final String url ="http://" + "37.18.4.44" + ":8080";
    private String result;
    private String peerName = "unity";

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void init() {
        Request request = new Request.Builder().get().url(url + "/sign_in?mobile").build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("http-response", "failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()){
                    result = response.body().string();
                    Log.d("http-response", result);
                    String[] clientNum = result.split("\n");
                    for (String element: clientNum){
                        String[] cur = element.split(",");
                        if (cur[0].equals("mobile")){
                            localID = cur[1];
                        }
                        else if (cur[0].equals(peerName)){
                            peerID = cur[1];
                        }
                    }
                    sendWait(localID);
                    if (peerID != null && localID != null){
                        callback.onSelfJoined();
                    }
                }
            }
        });
    }

    public void sendWait(String client_id){
        if (client_id != null){
            Request request = new Request.Builder().get().url(url + "/wait?peer_id=" + client_id).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("http-wait-response", "failure");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        result = response.body().string();
                        Log.d("http-wait-response", result);
                        if (result.startsWith(peerName)){
                            peerID = result.split(",")[1];
                            sendWait(localID);
                        }
                        else if (result.startsWith("\"{\\\"sdpMLineIndex\\\"")){
                            JSONObject jsonObject = JSON.parseObject(changeString(result));
                            //Candidate candidate = JSON.parseObject(result, Candidate.class);
                            callback.onIceCandidateReceived(jsonObject);
                            sendWait(localID);
                        }
                        else if (result.startsWith("\"{\\\"sdp\\\"")){
                            JSONObject jsonObject = JSON.parseObject(changeString(result));
                            if (jsonObject.getString("type").equals("offer")){
                                callback.onOfferReceived(jsonObject);
                                sendWait(localID);
                            }
                            else if (jsonObject.getString("type").equals("answer")){
                                callback.onAnswerReceived(jsonObject);
                                sendWait(localID);
                            }
                        }
                    }
                }
            });
        }
    }

    private String changeString(String result){
        String replaceAllr = result.replaceAll("\\\\r", "\r");
        //System.out.println("replaceAll:"+replaceAllr);

        String replaceAlln = replaceAllr.replaceAll("\\\\n", "\n");
        //System.out.println("replaceAll:"+replaceAlln);

        String substring = replaceAlln.substring(1, replaceAlln.length() - 1);
        //System.out.println("substring:"+substring);

        String substring2 = substring.replaceAll("\\\\", "");
        //System.out.println("substring:"+substring2);

        return substring2;
    }

    public void sendSignOut(){
        if (localID != null){
            Request request = new Request.Builder().get().url(url + "/sign_out?peer_id=" + localID).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("http-signout-response", "failure");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        result = response.body().string();
                        Log.d("http-signout-response", result);
                    }
                }
            });
        }
    }

    public void sendIceCandidate(IceCandidate iceCandidate) {
        JSONObject jo = new JSONObject();
        jo.put("candidate", iceCandidate.sdp);
        jo.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
        jo.put("sdpMid", iceCandidate.sdpMid);

        RequestBody body = RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), String.valueOf(jo));
        Request request = new Request.Builder().post(body).url(url + "/message?peer_id=" + localID + "&to=" + peerID).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("http-IceCandidate", "failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    result = response.body().string();
                    Log.d("http-IceCandidate", result);
                }
            }
        });
    }

    public void sendOfferSessionDescription(SessionDescription sdp) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("sdp", sdp.description);
        jo.put("type", "offer");
        RequestBody body = RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), String.valueOf(jo));
        Request request = new Request.Builder().post(body).url(url + "/message?peer_id=" + localID + "&to=" + peerID).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("http-send-offer", "failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    result = response.body().string();
                    Log.d("http-send-offer", result);
                }
            }
        });
    }

    public void sendAnswerSessionDescription(SessionDescription sdp) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("sdp", sdp.description);
        jo.put("type", "answer");

        RequestBody body = RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), String.valueOf(jo));
        Request request = new Request.Builder().post(body).url(url + "/message?peer_id=" + localID + "&to=" + peerID).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("http-send-answer", "failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    result = response.body().string();
                    Log.d("http-send-answer", result);
                }
            }
        });
    }

    public interface Callback {
        void onCreateRoom();
        void onPeerJoined();
        void onSelfJoined();
        void onPeerLeave(String msg);
        void onSelfLeave();
        void onOfferReceived(JSONObject data);
        void onAnswerReceived(JSONObject data);
        void onIceCandidateReceived(JSONObject data);
    }

}

