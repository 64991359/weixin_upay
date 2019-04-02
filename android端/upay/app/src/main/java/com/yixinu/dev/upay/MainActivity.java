package com.yixinu.dev.upay;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class MainActivity extends CommonActivity {

    private TextView mTextMessage;
    public EditText wxid;
    public EditText wx_user_name;
    public String WXID;
    public String WX_USER_NAME;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            LinearLayout mainlayout = (LinearLayout)findViewById(R.id.mainlayout);
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
                case R.id.navigation_set:
                    mTextMessage.setText("set");
                    /*
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("user_id");
                    editor.remove("device_id");
                    editor.remove("token");
                    editor.commit();
                    jump_logout();
                    */
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        String user_id = preferences.getString("user_id", "null");


        //检测有没有登录
        if( user_id.equals("null") || user_id.isEmpty() ){
            //没有登录
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        */

        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        wxid = (EditText) findViewById(R.id.wxid);
        wx_user_name = (EditText) findViewById(R.id.wx_user_name);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //把item宽度调到正常
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        init();
    }

    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                String wxid_str = msg.getData().getString("wxid");
                String wx_user_name_str = msg.getData().getString("wx_user_name");
                wxid.setText(wxid_str);
                wx_user_name.setText(wx_user_name_str);
            }
        }
    };

    /**
     * 初始化
     */
    protected void init(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    ServerSocket serverSocket = new ServerSocket(9988);
                    Socket socket = serverSocket.accept();
                    InputStream is = socket.getInputStream();     //获取输入流
                    BufferedReader bufReader = new BufferedReader(new InputStreamReader(is));
                    String s = null;
                    while ((s = bufReader.readLine()) != null) {
                        JSONObject json = new JSONObject(s);
                        String flag = json.getString("flag");
                        if(flag.equals("wx")){
                            String wxid_str = json.getString("wxid");
                            String mobile_str = json.getString("mobile");
                            Message msg = new Message();
                            msg.what = 1;   //接收微信ID和登录账号
                            Bundle bundle = new Bundle();
                            bundle.putString("wxid",wxid_str);
                            bundle.putString("wx_user_name",mobile_str);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }


    /*
    @Override
    public void handle_socket(String data, String flag) {
        super.handle_socket(data, flag);
        if(flag.equals("close")){
            showToast(data);
            jump_logout();
        }
    }

    @Override
    public void handle_socket_order(String money, String mark) {
        super.handle_socket_order(money, mark);
        Log.i("upay","收到订单");
    }
    */


    /**
     * 把item宽度调到正常
     */
    public static class BottomNavigationViewHelper {
        @SuppressLint("RestrictedApi")
        public static void disableShiftMode(BottomNavigationView view) {
            BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
            try {
                Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
                shiftingMode.setAccessible(true);
                shiftingMode.setBoolean(menuView, false);
                shiftingMode.setAccessible(false);
                for (int i = 0; i < menuView.getChildCount(); i++) {
                    BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                    //noinspection RestrictedApi
                    item.setShiftingMode(false);
                    // set once again checked value, so view will be updated
                    //noinspection RestrictedApi
                    item.setChecked(item.getItemData().isChecked());
                }
            } catch (NoSuchFieldException e) {
                Log.e("BNVHelper", "Unable to get shift mode field", e);
            } catch (IllegalAccessException e) {
                Log.e("BNVHelper", "Unable to change value of shift mode", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
