package com.yixinu.dev.upay;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.yixinu.dev.upay.service.IService;
import com.yixinu.dev.upay.service.SocketService;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonActivity extends AppCompatActivity {

    public SharedPreferences preferences;
    protected String key = "";  //登录后从服务端获取

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //设置固定竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        preferences = getSharedPreferences("com.yixinu.dev.upay", MODE_PRIVATE);
        //preferences.getString("key", "0");
        //SharedPreferences.Editor editor = preferences.edit();
        //editor.putString("clerk_name", "");
        //editor.commit();


        //begin 注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("receive_socket_message");
        //广播接收器
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction()=="receive_socket_message"){
                    //接收服务器发来的消息
                    String data = intent.getStringExtra("data");
                    String flag = intent.getStringExtra("flag");
                    handle_socket(data,flag);
                }
            }
        }, intentFilter);


        IntentFilter intentFilter_order = new IntentFilter();
        intentFilter_order.addAction("action_pull_black");
        //广播接收器
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction()=="action_pull_black"){
                    //接收服务器发来的消息
                    String money = intent.getStringExtra("money");
                    String mark = intent.getStringExtra("mark");
                    handle_socket_order(money,mark);
                }
            }
        }, intentFilter_order);
        //end

    }

    public void handle_socket_order(String money , String mark){

    }

    /**
     * 处理soket消息
     * @param data
     * @param flag
     */
    public void handle_socket(String data,String flag){

    }

    public void jump_logout(){
        Intent intent = new Intent(CommonActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    protected IService socketService;
    protected SocketConnection conn;

    /**
     * 绑定 socket 服务
     */
    protected void bindSocketService() {
        conn = new SocketConnection();
        Intent intent = new Intent(CommonActivity.this, SocketService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
        startService(intent);
    }

    /**
     * 继承 ServiceConnection 类
     */
    public class SocketConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            socketService = (IService) arg1;
            mHandler.sendEmptyMessage(200);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }

    }

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 200) {
                //
            } else if (msg.what == 0) {
                String content = (String) msg.obj;
                //
            }
        }
    };

    public String md5(String inputStr) {
        String md5Str = inputStr;
        try {
            if (inputStr != null) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(inputStr.getBytes());
                BigInteger hash = new BigInteger(1, md.digest());
                md5Str = hash.toString(16);
                if ((md5Str.length() % 2) != 0) {
                    md5Str = "0" + md5Str;
                }
            }
        } catch (Exception e) {

        }
        return md5Str;
    }

    /**
     * 获取系统时间的10位的时间戳
     * @return
     */
    public String getTime() {
        long time = System.currentTimeMillis() / 1000;    //获取系统时间的10位的时间戳
        String str = String.valueOf(time);
        return str;
    }

    public String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String str = formatter.format(curDate);
        return str;
    }

    //Toast 重写
    public void showToast(String str) {
        if (!TextUtils.isEmpty(str)) {
            Toast.makeText(CommonActivity.this, str, Toast.LENGTH_LONG).show();
        }
    }

}
