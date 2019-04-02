package com.yixinu.dev.upay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tsy.sdk.myokhttp.MyOkHttp;
import com.tsy.sdk.myokhttp.response.JsonResponseHandler;

import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;

import okhttp3.OkHttpClient;

public class LoginActivity extends AppCompatActivity {

    protected SharedPreferences preferences;
    public Button login_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        preferences = getSharedPreferences("com.yixinu.dev.upay", MODE_PRIVATE);
        login_btn = (Button) findViewById(R.id.login_btn);

        String user_id = preferences.getString("user_id", "null");
        if( user_id.equals("null") || user_id.isEmpty() ){
            //没有登录
        }else{
            //已经登录
            login_jump();
        }
    }

    protected void login_jump(){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void login_submit(View view){

        EditText login_user_edit = (EditText) findViewById(R.id.login_user);
        EditText login_pwd_edit = (EditText) findViewById(R.id.login_pwd);
        final String username = login_user_edit.getText().toString().trim();
        final String pwd = login_pwd_edit.getText().toString().trim();

        if(username.isEmpty()){
            showToast("请输入设备账号！");
            login_user_edit.requestFocus();
            return;
        }

        if(pwd.isEmpty()){
            showToast("请输入账号密码！");
            login_pwd_edit.requestFocus();
            return;
        }

        //设置按钮状态
        setbutton(2, "loading..");

        new Thread(){

            @Override
            public void run() {
                super.run();

                String rand = getTime();
                //String sign = md5(rand+key);
                HashMap<String, String> params = new HashMap<>();

                //params.put("rand", rand);
                params.put("user", username);
                params.put("pwd", pwd);
                //params.put("sign", sign);

                OkHttpClient okhttpclient = new OkHttpClient.Builder().build();
                MyOkHttp http = new MyOkHttp(okhttpclient);
                String url = "http://upay.yixinu.com/login";

                http.post().url(url).params(params).tag(this)
                        .enqueue(new JsonResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, JSONObject response) {
                                try {
                                    int errcode;
                                    String errmsg;
                                    errcode = response.getInt("code");
                                    errmsg = response.getString("msg");
                                    if (errcode == 0) {
                                        String data_str = response.getString("data");
                                        JSONObject d = new JSONObject(data_str);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString("user_id", d.getString("user_id"));
                                        editor.putString("device_id", d.getString("device_id"));
                                        editor.putString("token", d.getString("token"));
                                        editor.commit();

                                        Intent it = new Intent();
                                        it.setClass(LoginActivity.this, MainActivity.class);
                                        startActivity(it);
                                        finish();
                                    } else if (errcode == 1) {
                                        showToast(errmsg);

                                        //begin 设置按钮还原
                                        Message msg = new Message();
                                        msg.what = 2;
                                        myHandler.sendMessage(msg);
                                        //end
                                    }
                                } catch (Exception e) {

                                    //begin 设置按钮还原
                                    Message msg = new Message();
                                    msg.what = 2;
                                    myHandler.sendMessage(msg);
                                    //end

                                    System.out.println("upay:"+response.toString());
                                    showToast("网络异常，登录失败");
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, String error_msg) {

                                //begin 设置按钮还原
                                Message msg = new Message();
                                msg.what = 2;
                                myHandler.sendMessage(msg);
                                //end

                                showToast("网络异常，登录失败");
                            }
                        });
            }


        }.start();
    }

    /**
     *
     * @param status  按钮状态： 1可点击，2不可点击
     * @param text  按钮上显示的文本
     */
    public void setbutton(int status, String text) {
        if (status == 1) {
            //还原
            login_btn.setBackgroundColor(Color.parseColor("#FF248DF8"));
            login_btn.setTextColor(Color.parseColor("#ffffff"));
            login_btn.setText(text);
            login_btn.setClickable(true);
        } else if (status == 2) {
            //正在提交
            login_btn.setClickable(false);
            login_btn.setBackgroundColor(Color.parseColor("#c7c7c7"));
            login_btn.setTextColor(Color.parseColor("#444444"));
            login_btn.setText(text);
        }
    }

    //在主线程创建Handler对象
    final Handler myHandler = new Handler() {
        @Override
        //重写handleMessage方法,根据msg中what的值判断是否执行后续操作
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                //登录成功
                setbutton(2, "登录成功");
            }
            if (msg.what == 2) {
                //还原
                setbutton(1, "登录");
            }
        }
    };


    public String getTime() {
        long time = System.currentTimeMillis() / 1000;    //获取系统时间的10位的时间戳
        String str = String.valueOf(time);
        return str;
    }


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

    //Toast 重写
    public void showToast(String str) {
        if (!TextUtils.isEmpty(str)) {
            Toast.makeText(LoginActivity.this, str, Toast.LENGTH_LONG).show();
        }
    }
}
