package com.example.objecttracking;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class UdpClientBiz {
    private String mServerIp = "37.18.4.44";
    private InetAddress mServerAddress;
    private int mServerPort = 49222;
    private DatagramSocket mSocket;

    //构造方法
    public UdpClientBiz(){
        try {
            mServerAddress = InetAddress.getByName(mServerIp);
            mSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public interface OnMsgReturnedListener{
        void onMsgReturned(String msg);
        void onError(Exception ex);
    }


    public void sendMsg(String msg ,OnMsgReturnedListener listener){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    byte[] clientMsgBytes = msg.getBytes();
                    DatagramPacket clientPacket = new DatagramPacket(clientMsgBytes,
                            clientMsgBytes.length,
                            mServerAddress,
                            mServerPort);
                    mSocket.connect(mServerAddress, mServerPort);
                    mSocket.setSoTimeout(5000);
                    while (mSocket.isConnected()){
                        Log.d("mSocket connected: ", String.valueOf(mSocket.isConnected()));
                        mSocket.send(clientPacket);

                        Log.d("RemoteSocketAddress: ", String.valueOf(mSocket.getRemoteSocketAddress()));
                        Log.d("LocalSocketAddress: ", String.valueOf(mSocket.getLocalSocketAddress()));
                        Log.d("mSocket send: ", String.valueOf(clientPacket));

                        byte[] buf = new byte[2048];
                        DatagramPacket serverMsgPacket = new DatagramPacket(buf, buf.length);
                        mSocket.receive(serverMsgPacket);
                        String serverMsg = new String(serverMsgPacket.getData(),0,serverMsgPacket.getLength());
                        listener.onMsgReturned(serverMsg);
                    }
                    //mSocket.close();
                } catch (Exception e) {
                    listener.onError(e);
                }
            }
        }.start();
    }

/*    public void onDestroy() throws IOException {
        if (mSocket != null) {
            mSocket.close();
        }

    }*/
}
