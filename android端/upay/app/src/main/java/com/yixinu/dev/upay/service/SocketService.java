package com.yixinu.dev.upay.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class SocketService extends Service  {

    protected Socket socket;
    protected String HOST = "192.168.2.100";    //服务器地址
    protected int PORT = 4188;  //服务器端口号
    protected BufferedReader reader;//读取数据
    protected PrintWriter writer;//写入数据
    protected String workStatus;// 当前工作状况，null 表示正在处理，success 表示处理成功，failure 表示处理失败
    protected long HEART_BEAT_RATE = 60 * 1000;    //心跳检测
    protected long sendTime = 0L;
    receivedata rd; //接收数据的线程对象
    heartbeat hd;   //进行心跳检测的线程对象
    protected String token = "";
    protected String userid = "";
    protected String deviceid = "";

    protected final IBinder mBinder = new SocketBinder();


    class SocketBinder extends Binder implements IService {
        @Override
        public boolean sendData(JSONObject json) {
            return sendMessage(json);
        }

        @Override
        public boolean sendData(String jsonstr) {
            return sendMessage(jsonstr);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectService();
        //取用户token
        SharedPreferences preferences = getSharedPreferences("com.yixinu.dev.upay", MODE_PRIVATE);
        token = preferences.getString("token", "0");
        userid = preferences.getString("user_id", "0");
        deviceid = preferences.getString("device_id", "0");
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 连接服务器
     */
    protected void connectService(){
        new Thread(){
            @Override
            public void run() {
                try {
                    socket = new Socket(HOST,PORT);
                    if(socket.isConnected()){
                        //连接成功
                        Log.i("upay","socket连接成功");
                        workStatus = "success";
                        reader = new BufferedReader(new InputStreamReader(
                                socket.getInputStream(), "UTF-8"));
                        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                                socket.getOutputStream(), "UTF-8")), true);

                        //通知服务器连接成功
                        String jsonstring = "{flag:\"client\",\"user_id\":\""+userid+"\",\"device_id\":\""+deviceid+"\",\"token\":\""+token+"\",\"data\":\"\"}";
                        JSONObject json = new JSONObject(jsonstring);
                        writer.println(json.toString());
                        writer.flush();
                        sendTime = System.currentTimeMillis();//每次发送成数据，就改一下最后成功发送的时间，节省心跳间隔时间

                        rd = new receivedata();
                        rd.start();

                        hd = new heartbeat();
                        hd.start();

                    }

                } catch (SocketException ex) {
                    Log.i("upay:", "socket 连接失败 ");
                    ex.printStackTrace();
                    workStatus = "failure";// 如果是网络连接出错了，则提示网络连接错误
                    return;
                } catch (SocketTimeoutException ex) {
                    Log.i("upay:", "socket 连接失败 ");
                    ex.printStackTrace();
                    workStatus = "failure";// 如果是网络连接出错了，则提示网络连接错误
                    return;
                } catch (Exception ex) {
                    Log.i("upay:", "socket 连接失败 ");
                    ex.printStackTrace();
                    workStatus = "failure";// 如果是网络连接出错了，则提示网络连接错误
                    return;
                }

            }
        }.start();
    }


    /**
     * 进行心跳检测的线程
     */
    protected  class heartbeat extends  Thread{
        @Override
        public void run() {
            super.run();
            while (true) {
                //begin 心跳检测
                long t = System.currentTimeMillis() - sendTime;
                if ( t >= HEART_BEAT_RATE) {
                    System.out.println("upay:开始  heartbeat");
                    long time = System.currentTimeMillis() / 1000;
                    String jsonstring = "{flag:\"heartbeat\",\"user_id\":\""+userid+"\",\"device_id\":\""+deviceid+"\",\"token\":\""+token+"\",\"data\":{\"time\": \""+time+"\"} }";
                    sendMessage(jsonstring);
                    sendTime = System.currentTimeMillis();
                }
                //end
            }
        }
    }


    /**
     * 接收服务端socket数据的线程
     */
    protected class receivedata extends Thread {
        @Override
        public void run() {
            super.run();
            try{
                while (true) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            String content;
                            if ((content = reader.readLine()) != null) {
                                getMessage(content);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.i("upay","while 异常错误");
                e.printStackTrace();
            }

        }
    }



    /**
     * 发送消息
     * @param json
     */
    public boolean sendMessage(JSONObject json){
        return send_data(json);
    }
    public boolean sendMessage(String jsonstring){
        try {
            JSONObject json = new JSONObject(jsonstring);
            return send_data(json);
        }catch (JSONException e) {
            Log.i("upay:", "json数据格式解析失败 ");
            e.printStackTrace();
        }
        return false;
    }
    public boolean send_data(JSONObject json){
        // 如果未连接到服务器，创建连接
        if (socket == null || socket.isClosed()) {
            try{
                socket = new Socket(HOST,PORT);
                //连接成功
                Log.i("upay","socket连接成功");
                workStatus = "success";
                reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), "UTF-8"));
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), "UTF-8")), true);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket.isClosed() || socket.isClosed()){
            try{
                socket = new Socket(HOST,PORT);
                //连接成功
                Log.i("upay","socket连接成功");
                workStatus = "success";
                reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), "UTF-8"));
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), "UTF-8")), true);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket.isClosed()){
            return false;
        }

        if (!socket.isOutputShutdown()) {// 输入输出流是否关闭
            try {
                workStatus = "success";
                writer.println(json.toString());
                writer.flush();
                sendTime = System.currentTimeMillis();//每次发送成数据，就改一下最后成功发送的时间，节省心跳间隔时间
                Log.i("upay","数据发送完成：" + json.toString());
                return true;
            } catch (Exception e) {
                Log.i("upay:", "发送数据失败");
                e.printStackTrace();
                workStatus = "failure";
            }
        } else {
            Log.i("upay:", "发送数据时发现socket输入输出流已经关闭");
            workStatus = "failure";
        }
        return false;
    }


    /**
     * 处理接收的消息
     * @param content
     */
    public void getMessage(String content){
        try {
            Log.i("upay","收到socket数据："+content);
            JSONObject json = new JSONObject(content);
            String flag = json.getString("flag");
            JSONObject data = json.getJSONObject("data");

            //如果是收到新订单，发送广播，通知 xposed 生成二维码
            if(flag.equals("order")) {
                Intent intent = new Intent();
                intent.setAction("action_pull_black");
                intent.putExtra("money", data.getString("money"));
                intent.putExtra("mark", data.getString("qrcode_id"));
                sendBroadcast(intent);
            }else{
                //如果是接收的普通消息
                Intent intent = new Intent();
                intent.setAction("receive_socket_message");
                intent.putExtra("data", json.getString("data"));
                intent.putExtra("flag", json.getString("flag"));
                sendBroadcast(intent);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            workStatus="failure";
        }
    }


}
