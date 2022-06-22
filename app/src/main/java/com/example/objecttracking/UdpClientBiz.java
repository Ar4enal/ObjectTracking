package com.example.objecttracking;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class UdpClientBiz {
    private String mServerIp = "37.18.4.44"; //37.18.4.44  192.168.201.133
    private InetAddress mServerAddress;
    private int mServerPort = 49222; //49222  60000
    private Socket mSocket;
    private OutputStream mOutStream;
    private InputStream mInStream;

    //构造方法
    public UdpClientBiz(){
        try {
            mServerAddress = InetAddress.getByName(mServerIp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public interface OnMsgReturnedListener{
        void onMsgReturned(byte[] msg);
        void onError(Exception ex);
    }


    public void sendMsg(String msg ,OnMsgReturnedListener listener){
        new Thread(){
            @Override
            public void run() {
                super.run();
                boolean socketConnected = false;
                while (!socketConnected) {
                    try {
                        Log.d("sendsocket", "connect");
                        byte[] clientMsgBytes = msg.getBytes();
                        mSocket = new Socket(mServerAddress, mServerPort);
                        if (mSocket != null) {
                            //获取输出流、输入流
                            mOutStream = mSocket.getOutputStream();
                            mInStream = mSocket.getInputStream();
                        }
                        mOutStream.write(clientMsgBytes);
                        mOutStream.flush();
                        Log.d("RemoteSocketAddress: ", String.valueOf(mSocket.getRemoteSocketAddress()));
                        Log.d("LocalSocketAddress: ", String.valueOf(mSocket.getLocalSocketAddress()));
                        if (mSocket.isConnected()){
                            socketConnected = true;
                        }
                        while (socketConnected) {
                            byte[] buf = new byte[4096];
                            recvMsg(buf, listener);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
    }

    public void recvMsg(byte[] buf, OnMsgReturnedListener listener){
        Log.d("mSocket is connected","receiving detections");
        try {
            mInStream.read(buf);
            listener.onMsgReturned(buf);
        } catch (Exception e) {
            listener.onError(e);
        }
    }



}
