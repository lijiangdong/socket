package com.example.ljd.socketclient;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Date;
import java.text.SimpleDateFormat;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity{

    private static final int RECEIVE_NEW_MESSAGE = 1;
    private static final int SOCKET_CONNECT_SUCCESS = 2;
    private static final int SOCKET_CONNECT_FAIL = 3;

    @Bind(R.id.msg_edit_text)
    EditText mMessageEditText;

    @Bind(R.id.show_linear)
    LinearLayout mShowLinear;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;
    private boolean mIsConnectServer = false;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case RECEIVE_NEW_MESSAGE:
                    TextView textView = new TextView(MainActivity.this);
                    textView.setText((String)msg.obj);
                    mShowLinear.addView(textView);
                    break;

                case SOCKET_CONNECT_SUCCESS:
                    Toast.makeText(MainActivity.this,"连接服务端成功",Toast.LENGTH_SHORT).show();
                    break;

                case SOCKET_CONNECT_FAIL:
                    Toast.makeText(MainActivity.this,"连接服务端失败，请重新尝试",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    @Override
    protected void onDestroy() {
        ButterKnife.unbind(this);
        disConnectServer();
        super.onDestroy();
    }

    @OnClick({R.id.send_btn,R.id.connect_btn,R.id.disconnect_btn})
    public void onClickButton(View v) {

        switch (v.getId()){
            case R.id.send_btn:
                sendMessageToServer();
                break;
            case R.id.connect_btn:
                new Thread() {
                    @Override
                    public void run() {
                        connectServer();
                    }
                }.start();
                break;
            case R.id.disconnect_btn:
                Toast.makeText(MainActivity.this,"已经断开连接",Toast.LENGTH_SHORT).show();
                disConnectServer();
                break;
        }

    }

    private String getTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    private void connectServer() {

        if (mIsConnectServer)
            return;

        int count = 0;
        while (mClientSocket == null) {
            try {
                mClientSocket = new Socket("10.10.14.160", 8688);
                mPrintWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(mClientSocket.getOutputStream())), true);
                mIsConnectServer = true;
                mHandler.obtainMessage(SOCKET_CONNECT_SUCCESS).sendToTarget();
            } catch (IOException e) {
                SystemClock.sleep(1000);
                count++;
                if (count == 5){
                    mHandler.obtainMessage(SOCKET_CONNECT_FAIL).sendToTarget();
                    return;
                }
            }
        }

        try {
            // 接收服务器端的消息
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    mClientSocket.getInputStream()));
            while (!MainActivity.this.isFinishing()) {
                String msg = bufferedReader.readLine();
                if (msg != null) {
                    String time = getTime(System.currentTimeMillis());
                    final String showedMsg = "server " + time + ":" + msg;
                    mHandler.obtainMessage(RECEIVE_NEW_MESSAGE, showedMsg)
                            .sendToTarget();
                }
            }
            mPrintWriter.close();
            bufferedReader.close();
            mClientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disConnectServer(){
        mIsConnectServer = false;
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessageToServer(){
        if (!mIsConnectServer){
            Toast.makeText(this,"没有连接上服务端，请重新连接",Toast.LENGTH_SHORT).show();
            return;
        }
        final String msg = mMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
            mPrintWriter.println(msg);
            mMessageEditText.setText("");
            String time = getTime(System.currentTimeMillis());
            final String showedMsg = "client " + time + ":" + msg;
            TextView textView = new TextView(this);
            textView.setText(showedMsg);
            mShowLinear.addView(textView);
        }
    }
}
